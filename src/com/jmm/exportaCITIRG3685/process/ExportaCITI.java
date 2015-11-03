/*
 * Proceso para generar los archivos exigidos por la RG3685: CITI compras y ventas
 * Diseño de los registros: http://www.afip.gob.ar/comprasyventas/
 * 
 * Autor: Juan Manuel Martínez - jmmartinezsf@gmail.com
 * Versión 0.1 - septiembre de 2015
 * Para Libertya 15.03
 */

package com.jmm.exportaCITIRG3685.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;

import org.openXpertya.model.MPeriod;
import org.openXpertya.process.ProcessInfoParameter;
import org.openXpertya.process.SvrProcess;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;

import com.jmm.exportaCITIRG3685.model.LP_C_Invoice;
import com.jmm.exportaCITIRG3685.model.LP_C_Tax;
import com.jmm.exportaCITIRG3685.model.MInvoice;

public class ExportaCITI extends SvrProcess {
  
    private MPeriod paramPeriodo;
    private String paramTipoTransaccion;
    private String paramCarpetaSalida = "";
    private ResultSet rs = null;
	private int invoiceId;
	boolean isSOTrx;
	private Double montoConsumidorFinal, montoIVA, montoIIBB
			, montoImpNacionales, montoImpMunicipales, montoImpOtros
			, montoOperacionesExentas; 

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");	
	private static final DecimalFormat decimalFormat = new DecimalFormat("#.00");
	
	private static final String QUERY = 			
			"select \n" + 
			"	inv.c_invoice_id\n" + 
			"	, inv.dateacct::date\n" + 
			"	, inv.dateinvoiced::date\n" + 
			"	, inv.afipdoctype\n" + 
			"	, inv.documentno\n" + 
			"	, inv.grandtotal\n" +
			"	, COALESCE(bp.taxidtype, '99') \n" + 
			"	, COALESCE(bp.taxid, inv.nroidentificcliente)\n" + 
			"	, bp.name\n" + 
			"	, sum(itax.taxbaseamt)\n" + 
			"	, sum(itax.taxamt)\n" + 
			"	, cur.wsfecode\n" + 
			"	, tax.citirg3685\n" +
			"	, tax.WSFEcode \n" +			
			"	, ltr.letra \n" +
			"from c_invoicetax itax \n" + 
			"join c_invoice inv on itax.c_invoice_id = inv.c_invoice_id \n" + 
			"join c_doctype dt on inv.c_doctype_id = dt.c_doctype_id \n" + 
			"join c_bpartner bp on inv.c_bpartner_id = bp.c_bpartner_id \n" + 
			"join c_currency cur on inv.c_currency_id = cur.c_currency_id \n" + 
			"join c_tax tax on itax.c_tax_id = tax.c_tax_id \n" + 
			"join c_letra_comprobante ltr on inv.c_letra_comprobante_id = ltr.c_letra_comprobante_id \n" + 
			"where \n" + 
			"	inv.dateacct between ? and ? \n" + 
			"	and (dt.docsubtypeinv in ('SF') or dt.isfiscaldocument = 'Y') \n" + 
			"	and inv.docstatus = 'CO' \n" + 
			"	and inv.issotrx = ? \n" + 
			"group by 1, 2, 3, 4, 5, 6, 7, 8, 9, 12 , 13, 14, 15 \n" +
			"order by \n" + 
			"	3 asc, inv.c_invoice_id, bp.name, inv.documentno, tax.citirg3685\n"
			;

	private static final int IX_INVOICE_ID 				= 1;
	private static final int IX_INVOICE_DATE_ACCT 		= 2;
	private static final int IX_INVOICE_DATE_INVOICED 	= 3;
	private static final int IX_INVOICE_AFIP_DOCTYPE 	= 4;
	private static final int IX_INVOICE_DOCUMENT_NO 	= 5;
	private static final int IX_INVOICE_GRANDTOTAL 		= 6;
	private static final int IX_BP_TAX_ID_TYPE 			= 7;
	private static final int IX_BP_TAX_ID 				= 8;
	private static final int IX_BP_NAME 				= 9;
	private static final int IX_INVOICE_TAX_AMT_BASE 	= 10;
	private static final int IX_INVOICE_TAX_AMT 		= 11;
	private static final int IX_CURRENCY_WSFECODE 		= 12;
	private static final int IX_TAX_CITI_REF 			= 13;
	private static final int IX_TAX_WSFE_CODE 			= 14;
	private static final int IX_LETRA 					= 15;
	
	protected void prepare() {
		borraImpuestos();
		
		ProcessInfoParameter[] para = getParameter();
		for(int i = 0;i < para.length;i++) {
			log.fine("prepare - " + para[i]);

            String name = para[i].getParameterName();

            if(para[i].getParameter() == null)
                ;
            else if (name.equalsIgnoreCase("Periodo"))
            	paramPeriodo = MPeriod.get(Env.getCtx(), Integer.valueOf((String)para[i].getParameter()), Env.getCtx().getProperty(name));
             else if(name.equalsIgnoreCase("TipoTrans")) 
            	paramTipoTransaccion = (String)para[i].getParameter();
             else if(name.equalsIgnoreCase("Directorio")) 
            	paramCarpetaSalida = (String)para[i].getParameter();
             else
                log.log(Level.SEVERE,"prepare - Parámetro desconocido: " + name);
        }

	}
	
	protected String doIt() throws java.lang.Exception {
		isSOTrx = !paramTipoTransaccion.equalsIgnoreCase("V") ;
		
		File targetDir = new File (paramCarpetaSalida);
		if (!targetDir.exists())
			targetDir.mkdir();
		
		String archivo_cbte = paramCarpetaSalida + File.separator + "REGINFO_CV_" + 
							  (isSOTrx ? "VENTAS" : "COMPRAS") + "_CBTE_" +
							  paramPeriodo.getName().toUpperCase() + ".txt";
		String archivo_alic = paramCarpetaSalida + File.separator + "REGINFO_CV_" + 
							  (isSOTrx ? "VENTAS" : "COMPRAS" )  + "_ALICUOTAS_" +
							  paramPeriodo.getName().toUpperCase() + ".txt";
		String result ="";
		String lineSeparator = System.getProperty("line.separator");
        FileWriter cbtes = null, alic = null;
        PreparedStatement pstmt = null;
 		
        try
 		{
 			long inicia = System.currentTimeMillis();
 			
 			/*
 			 * Necesito poder navegar hacia adelante y atrás en el resulset, por eso creo de esta manera
 			 * el preparestatement.
 			 */
 			pstmt = DB.getConnectionRW().prepareStatement(QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
 			pstmt.setTimestamp(1, paramPeriodo.getStartDate());
 			pstmt.setTimestamp(2, paramPeriodo.getEndDate());
 			pstmt.setString(3, isSOTrx ? "Y" : "N");
 			rs = pstmt.executeQuery();
 			
 			cbtes = new FileWriter(archivo_cbte);
 			alic = new FileWriter(archivo_alic);
 			int Q = creaArchivos(cbtes, alic);
 			cbtes.close();
 			alic.close();
		
 			long termina = System.currentTimeMillis();
 			long segundos = (termina-inicia)/1000;
 			log.log(Level.SEVERE,"ExportaCITI RG3685 Tardo: "+segundos+" segundos - " + Q + " registros.");
 			rs.close();
 			pstmt.close();
 			pstmt = null;
 		}
 		catch (Exception e)
 		{ 
 			log.saveError("Exportacion CITI RG3685 - Prepare", e);
 			e.printStackTrace();
 			
 			try {
				if(cbtes != null) cbtes.close();
				if(alic != null) alic.close();
				if(pstmt != null) pstmt.close();
				if(rs != null)rs.close();					
			} catch (IOException e1) {
				result = "Error al generar los archivos CITI RG3685 " +lineSeparator + e.getLocalizedMessage();
				e1.printStackTrace();
			}	
 		}	
 		return result;
	}				
	
	private int creaArchivos(FileWriter fw_c , FileWriter fw_a) throws Exception
	{
		String lineSeparator = System.getProperty("line.separator");
		int cant = 0;
		int q_alic = 0;
		
		StringBuffer la; 				// linea de alícuotas
		StringBuffer lc; 				// línea de comprobantes
		
		String cmpFecha;
		String cmpTipo ;
		String cmpPuntoVenta;
		String cmpNumero;
		String cmpLetra;
		String cmpTotal ;
		
		String bpCodigoIdentificadorFiscal;
		String bpIdentificadorFiscal;
		String bpNombre;
		
		String monedaCodigoWSFE;
		String citiReference = "";

		
		while(rs.next())
		{
			/* 
			 * Primero genero las líneas de las alícuotas y voy calculando las sumas totalizadoras del
			 * informe por comprobante, que va en un archivo separado.
			*/

			cmpFecha = formatDate(rs.getDate(IX_INVOICE_DATE_INVOICED));
			cmpTipo = pad(rs.getString(IX_INVOICE_AFIP_DOCTYPE), 3, true);
			cmpPuntoVenta = pad(rs.getString(IX_INVOICE_DOCUMENT_NO).substring(1, 5), 5, true);
			cmpNumero = pad(rs.getString(IX_INVOICE_DOCUMENT_NO).substring(6, 13), 20, true);
			bpCodigoIdentificadorFiscal = rs.getString(IX_BP_TAX_ID_TYPE);
			bpIdentificadorFiscal = pad(rs.getString(IX_BP_TAX_ID).replace("-", ""), 20, true);
			cmpTotal = pad(formatAmount(rs.getDouble(IX_INVOICE_GRANDTOTAL)), 15, true);
			monedaCodigoWSFE = rs.getString(IX_CURRENCY_WSFECODE);
			bpNombre = pad(rs.getString(IX_BP_NAME).toUpperCase(), 30, false);
			cmpLetra = rs.getString(IX_LETRA).toUpperCase();
			
			citiReference = rs.getString(IX_TAX_CITI_REF);
			
			if(citiReference==null)
				throw new Exception("No se pudo determinar el código de impuesto para " + rs.getString(IX_INVOICE_DOCUMENT_NO));
			citiReference = citiReference.trim().toUpperCase();

			if (cmpLetra.equals("A") || cmpLetra.equals("B") || cmpLetra.equals("M") || esOtros(cmpTipo)){
				boolean write = false;
				la = new StringBuffer();
				la.append(cmpTipo);
 				if (esOtros(cmpTipo))
 	 				la.append("00000");
 				else
 	 				la.append(cmpPuntoVenta);
 				la.append(cmpNumero);
 				if (!isSOTrx){			// los campos 6 y 7 no van para informes de ventas
 					la.append(bpCodigoIdentificadorFiscal);
 					la.append(bpIdentificadorFiscal);
 				}

				if (esCreditoDebitoFiscal(citiReference)){
	 				la.append(pad(formatAmount(rs.getDouble(IX_INVOICE_TAX_AMT_BASE)), 15, true));		// NG
	 				la.append(pad(rs.getString(IX_TAX_WSFE_CODE), 4, true));					// Alícuota de IVA
	 				la.append(pad(formatAmount(rs.getDouble(IX_INVOICE_TAX_AMT)), 15, true));		// IVA liquidado
	 				write = true;
				}else if (montoConsumidorFinal == 0.0 && 
						citiReference.equals(LP_C_Tax.CITIRG3685_ImportesExentos)){
					
					// 	Montos no gravados en Fac A o M					
					la.append(pad(formatAmount(rs.getDouble(IX_INVOICE_TAX_AMT_BASE)), 15, true));	// Monto no gravado
					la.append(pad(rs.getString(IX_TAX_WSFE_CODE), 4, true));				// Alícuota de IVA
	 				la.append(pad("0", 15, true));
	 				write = true;
				}
				if(write){
	 				q_alic++;
	 				la.append(lineSeparator);
	 				fw_a.write(la.toString());					
				}
				la = null;				
			}
			
			/*
			 * AcumulaImportes agrega los montos de impuestos a cada variable. Si devuelve verdadero, es porque
			 * la próxima línea es de otro comprobante por lo que debo generar la línea del comprobante. Lo mismo
			 * ocurre si es la última línea del rs.
			 */
			if (acumulaImportes(rs.getInt(IX_INVOICE_ID)) || rs.isLast()){
				lc = new StringBuffer();
				lc.append(cmpFecha);
				lc.append(cmpTipo);
				if (esOtros(cmpTipo))
					lc.append("00000");
				else
					lc.append(cmpPuntoVenta);
				lc.append(cmpNumero);
				if (!isSOTrx)
					// TODO: las importaciones no están soportadas por esta versión.
					lc.append("                ");
				else
					lc.append(cmpNumero);
				lc.append(bpCodigoIdentificadorFiscal);
				lc.append(bpIdentificadorFiscal);
				lc.append(bpNombre);
				lc.append(cmpTotal);
				// TODO: no se discriminan conceptos no gravados de exentos en esta versión.
				lc.append("000000000000000");
				if (cmpLetra.equals("C") || cmpLetra.equals("B") ||
						((cmpLetra.equals("A") || esOtros(cmpTipo)) && montoConsumidorFinal == 0.0))
					lc.append("000000000000000");
				else
					lc.append(pad(formatAmount(montoOperacionesExentas), 15, true));
				lc.append(pad(formatAmount(montoIVA), 15, true));
				lc.append(pad(formatAmount(montoImpNacionales), 15, true));
				lc.append(pad(formatAmount(montoIIBB), 15, true));
				lc.append(pad(formatAmount(montoImpMunicipales), 15, true));
				// TODO: dar soporte a impuestos internos
				lc.append("000000000000000");
				// TODO: dar soporte multimoneda
				lc.append(monedaCodigoWSFE);
				lc.append("0001000000");  // Tipo de cambio fijo, ya que no hay multimoneda
				lc.append(q_alic);
				if ((cmpLetra.equals("A") || esOtros(cmpTipo)) && montoConsumidorFinal == 0.0)
					lc.append("E");
				else
					lc.append("0");
				if (!isSOTrx)
					lc.append(pad(formatAmount(montoConsumidorFinal), 15, true));
				lc.append(pad(formatAmount(montoImpOtros), 15, true));
				if (!isSOTrx)
					// TODO: dar soporte para comisiones de corredores
					lc.append("00000000000                              000000000000000");
				if (isSOTrx)
					lc.append(cmpFecha);
				lc.append(lineSeparator);
 				fw_c.write(lc.toString());
 				//fw_c.write("\r");
 				lc = null;
 				q_alic = 0;
			}
			
			/*
			 * Guardo el  invoice_id del comprobante por que si en la próxima línea del rs 
			 * el invoice_id tiene el mismo valor entonces tengo que acumular los montos de impuestos.
			 */
			invoiceId = rs.getInt(IX_INVOICE_ID);
			cant++;
		}
	
		return cant;
			
	}
	
	/*
	 * Retorna la fecha en formato anio mes dia
	 */
    private String formatDate(Date fecha) {
        return dateFormat.format(fecha);
    }

    /*
     * Retorna el monto como un string sin el punto decimal 
     */
    private String formatAmount(Double amt) {
    	return decimalFormat.format(amt).replace(".", "").replace(",", "");
    }
    
    /*
     * Agrega la cantidad necesaria de ceros o espacios al inicio del argumento src
     * para completar el largo indicado
     * flag = true: agrega ceros
     * flag = false: agrega espacios
     */
    private String pad(String src, int largo, Boolean flag){
    	
    	StringBuffer ret = new StringBuffer();
    	String r = new String();
    	
    	if (src == null)
    		src = "";
    	
    	int n = largo - src.length();
    	if (n < 0){
    		r = src.substring(0, largo - 1);
    		n = 1;
    	}
    	else
    		r = src;
    	
    	final char[] arr = new char[n];
    	Arrays.fill(arr, flag?'0':' ');
    	
    	ret.append(arr);
    	ret.append(r);
    	return ret.toString();
    }
    
    /*
     * Borra los montos acumulados 
     */
    private void borraImpuestos(){
    	montoConsumidorFinal = 0.0;
    	montoIVA = 0.0;
    	montoIIBB = 0.0;
    	montoImpNacionales = 0.0;
    	montoImpMunicipales = 0.0;
    	montoImpOtros = 0.0;
    	montoOperacionesExentas = 0.00;
    }
    
    /*
     * Devuelve verdadero si el id del impuesto consultado corresponde a uno configurado 
     * como crédito o débto fiscal
     */
    private Boolean esCreditoDebitoFiscal(String reference){
    	return reference.equals(LP_C_Tax.CITIRG3685_CréditoODébitoFiscalIVA);
    }
    
    /*
     * Devuelve verdadero si el tipo de comprobante es "Otros comprobantes" u "Otros comprobantes - credito".
     */
    private Boolean esOtros(String tipo){
    	return tipo.equals(MInvoice.AFIPDOCTYPE_OtrosComprobantes_NotasDeCrédito) 
    			|| tipo.equals(MInvoice.AFIPDOCTYPE_OtrosComprobantes);
    }
    
    /*
     * Acumula los importes de las líneas de cada factura.
     * Devuelve:
     *   true si la próxima línea del rs corresponde a la misma factura que la actual línea
     *   false en caso contrario
     */
    private Boolean acumulaImportes(int id) throws SQLException{
    	
    	Boolean ret = false;

    	/*
		 * Chequeo si el invoice_id de ésta línea es el mismo que el que está guardado y reseteo los montos
		 * de impuestos de ser necesario
		 */
		if (invoiceId != rs.getInt(IX_INVOICE_ID))
			borraImpuestos();

		if (rs.isLast()) // Si estoy en la última línea, devuelvo true para que se guarde el comprobante.
    		ret = true;
		else{
	    	rs.next();
	    	ret = !(rs.getInt(IX_INVOICE_ID) == id);
	    	rs.previous();
		}
		
		Double taxAmount = rs.getDouble(IX_INVOICE_TAX_AMT);
		Double taxBaseAmount = rs.getDouble(IX_INVOICE_TAX_AMT_BASE);
		
		String reference = rs.getString(IX_TAX_CITI_REF).trim().toUpperCase();
		if (reference.equals(LP_C_Tax.CITIRG3685_CréditoODébitoFiscalIVA))
			montoConsumidorFinal += taxAmount;
		
		else if (reference.equals(LP_C_Tax.PERCEPTIONTYPE_IVA))
			montoIVA += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeIngresosBrutos))
			montoIIBB += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeImpuestosNacionales))
			montoImpNacionales += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeImpuestosMunicipales))
			montoImpMunicipales += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_OtrosImpuestos))
			montoImpOtros += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_ImportesExentos))
			montoOperacionesExentas += taxBaseAmount;
		
		return ret;
    }
}
