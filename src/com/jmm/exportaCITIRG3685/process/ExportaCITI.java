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

import com.jmm.exportaCITIRG3685.model.LP_C_Tax;

public class ExportaCITI extends SvrProcess {
  
    private MPeriod periodo;
    private String transaction;
    private String directorio = "";
    private ResultSet rs = null;
	private Double cf, p_iva, p_iibb, p_nac, p_mun, ot, ex; 
	private int invoice_id;
	
	private static final String QUERY = "select \n" + 
			"	inv.c_invoice_id\n" + 
			"	, inv.dateacct::date\n" + 
			"	, inv.dateinvoiced::date\n" + 
			"	, inv.afipdoctype\n" + 
			"	, inv.documentno\n" + 
			"	, inv.grandtotal\n" +
			"	, COALESCE(bp.taxidtype, '99') \n" + 
			"	, COALESCE(bp.taxid, inv.nroidentificcliente)\n" + 
			"	, bp.name\n" + 
			"	, itax.taxbaseamt\n" + 
			"	, itax.taxamt\n" + 
			"	, cur.wsfecode\n" + 
			"	, tax.citirg3685\n" + 
			"	, to_char(tax.rate, '90.00'), \n" + 
			"	ltr.letra, tax.WSFEcode \n" + 
			"from libertya.c_invoicetax itax \n" + 
			"join libertya.c_invoice inv on itax.c_invoice_id = inv.c_invoice_id \n" + 
			"join libertya.c_doctype dt on inv.c_doctype_id = dt.c_doctype_id \n" + 
			"join libertya.c_bpartner bp on inv.c_bpartner_id = bp.c_bpartner_id \n" + 
			"join libertya.c_currency cur on inv.c_currency_id = cur.c_currency_id \n" + 
			"join libertya.c_tax tax on itax.c_tax_id = tax.c_tax_id \n" + 
			"join libertya.c_letra_comprobante ltr on inv.c_letra_comprobante_id = ltr.c_letra_comprobante_id \n" + 
			"where \n" + 
			"	inv.dateacct between ? and ? and \n" + 
			"	inv.c_doctype_id not in (1010517, 1010518, 1010519, 1010520) \n" + 
			"	and inv.docstatus = 'CO' \n" + 
			"	and inv.issotrx = ? \n" + 
			"order by \n" + 
			"	inv.dateinvoiced asc, bp.name, inv.documentno, tax.citirg3685\n"
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
	private static final int IX_TAX_RATE 				= 14;
	private static final int IX_TAX_WSFE_CODE 			= 15;
	private static final int IX_LETRA 					= 16;
	
	protected void prepare() {
		borraImpuestos();
		
		ProcessInfoParameter[] para = getParameter();
		for(int i = 0;i < para.length;i++) {
			log.fine("prepare - " + para[i]);

            String name = para[i].getParameterName();

            if(para[i].getParameter() == null)
                ;
            else if (name.equalsIgnoreCase("Periodo"))
            	periodo = MPeriod.get(Env.getCtx(), ((BigDecimal) para[i].getParameter()).intValueExact(), Env.getCtx().getProperty(name));
             else if(name.equalsIgnoreCase("TipoTrans")) 
            	transaction = (String)para[i].getParameter();
             else if(name.equalsIgnoreCase("Directorio")) 
            	directorio = (String)para[i].getParameter();
             else
                log.log(Level.SEVERE,"prepare - Parámetro desconocido: " + name);
        }

	}
	
	protected String doIt() throws java.lang.Exception {
		
		File targetDir = new File (directorio);
		if (!targetDir.exists())
			targetDir.mkdir();
		
		String archivo_cbte = directorio + "/REGINFO_CV_" + 
							  (transaction.equalsIgnoreCase("V") ? "COMPRAS":"VENTAS") + "_CBTE_" +
							  periodo.getName().toUpperCase() + ".txt";
		String archivo_alic = directorio + "/REGINFO_CV_" + 
							  (transaction.equalsIgnoreCase("V") ? "COMPRAS":"VENTAS")  + "_ALICUOTAS_" +
							  periodo.getName().toUpperCase() + ".txt";
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
 			/*pstmt.setTimestamp(1, date_from);
 			pstmt.setTimestamp(2, date_to);*/
 			pstmt.setTimestamp(1, periodo.getStartDate());
 			pstmt.setTimestamp(2, periodo.getEndDate());
 			pstmt.setString(3, (transaction.equalsIgnoreCase("V")?"N":"Y"));
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
	
	private int creaArchivos(FileWriter fw_c , FileWriter fw_a) throws IOException, SQLException
	{
		String lineSeparator = System.getProperty("line.separator");
		int cant = 0;
		int q_alic = 0;
		StringBuffer s; 				// linea de alícuotas
		StringBuffer c; 				// línea de comprobantes
		String fecha = new String();
		String tipo_comp = new String();
		String pv = new String();
		String nro = new String();
		String cod_doc = new String();
		String nro_doc = new String();
		String total = new String();
		String moneda = new String();
		String rz = new String();
		String letra = new String();
		
		while(rs.next())
		{
			/* 
			 * Primero genero las líneas de las alícuotas y voy calculando las sumas totalizadoras del
			 * informe por comprobante, que va en un archivo separado.
			*/

			fecha = formatDate(rs.getDate(IX_INVOICE_DATE_INVOICED));
			tipo_comp = pad(rs.getString(IX_INVOICE_AFIP_DOCTYPE), 3, true);
			pv = pad(rs.getString(IX_INVOICE_DOCUMENT_NO).substring(1, 5), 5, true);
			nro = pad(rs.getString(IX_INVOICE_DOCUMENT_NO).substring(6, 13), 20, true);
			cod_doc = rs.getString(IX_BP_TAX_ID_TYPE); // TODO: Revisar
			nro_doc = pad(rs.getString(IX_BP_TAX_ID).replace("-", ""), 20, true);
			total = pad(formatAmount(rs.getDouble(IX_INVOICE_GRANDTOTAL)), 15, true);
			moneda = rs.getString(IX_CURRENCY_WSFECODE);
			rz = pad(rs.getString(IX_BP_NAME).toUpperCase(), 30, false);
			letra = rs.getString(IX_LETRA);			

			if (letra.equalsIgnoreCase("A") || letra.equalsIgnoreCase("B") ||letra.equalsIgnoreCase("M") || esOtros(tipo_comp)){ 
				s = new StringBuffer();
				s.append(tipo_comp);
 				if (esOtros(tipo_comp))
 	 				s.append("00000");
 				else
 	 				s.append(pv);
 				s.append(nro);
 				if (transaction.equalsIgnoreCase("V")){			// los campos 6 y 7 no van para informes de ventas
 					s.append(cod_doc);
 					s.append(nro_doc);
 				}

				if (esCreditoDebitoFiscal(rs.getString(IX_TAX_CITI_REF))){
	 				s.append(pad(formatAmount(rs.getDouble(IX_INVOICE_TAX_AMT_BASE)), 15, true));		// NG
	 				s.append(pad(rs.getString(IX_TAX_WSFE_CODE), 4, true));					// Alícuota de IVA
	 				s.append(pad(formatAmount(rs.getDouble(IX_INVOICE_TAX_AMT)), 15, true));		// IVA liquidado
	 				q_alic++;
	 				s.append(lineSeparator);
	 				fw_a.write(s.toString());
				}else{															// Montos no gravados en Fac A o M 
					if (cf == 0.0 && rs.getString(IX_TAX_CITI_REF).equalsIgnoreCase("EXE")){
						s.append(pad(formatAmount(rs.getDouble(IX_INVOICE_TAX_AMT_BASE)), 15, true));	// Monto no gravado
						s.append(pad(rs.getString(IX_TAX_WSFE_CODE), 4, true));				// Alícuota de IVA
		 				s.append(pad("0", 15, true));
		 				q_alic++;
		 				s.append(lineSeparator);
		 				fw_a.write(s.toString());
					}
				}
				s = null;
				//fw_a.write("\r");
			}
			
			/*
			 * AcumulaImportes agrega los montos de impuestos a cada variable. Si devuelve verdadero, es porque
			 * la próxima línea es de otro comprobante por lo que debo generar la línea del comprobante. Lo mismo
			 * ocurre si es la última línea del rs.
			 */
			if (acumulaImportes(rs.getInt(IX_INVOICE_ID)) || rs.isLast()){
				c = new StringBuffer();
				c.append(fecha);
				c.append(tipo_comp);
				if (esOtros(tipo_comp))
					c.append("00000");
				else
					c.append(pv);
				c.append(nro);
				if (transaction.equalsIgnoreCase("V"))
					// TODO: las importaciones no están soportadas por esta versión.
					c.append("                ");
				else
					c.append(nro);
				c.append(cod_doc);
				c.append(nro_doc);
				c.append(rz);
				c.append(total);
				// TODO: no se discriminan conceptos no gravados de exentos en esta versión.
				c.append("000000000000000");
				if (letra.equalsIgnoreCase("C") || letra.equalsIgnoreCase("B") ||
						((letra.equalsIgnoreCase("A") || esOtros(tipo_comp)) && cf == 0.0))
					c.append("000000000000000");
				else
					c.append(pad(formatAmount(ex), 15, true));
				c.append(pad(formatAmount(p_iva), 15, true));
				c.append(pad(formatAmount(p_nac), 15, true));
				c.append(pad(formatAmount(p_iibb), 15, true));
				c.append(pad(formatAmount(p_mun), 15, true));
				// TODO: dar soporte a impuestos internos
				c.append("000000000000000");
				// TODO: dar soporte multimoneda
				c.append(moneda);
				c.append("0001000000");  // Tipo de cambio fijo, ya que no hay multimoneda
				c.append(q_alic);
				if ((letra.equalsIgnoreCase("A") || esOtros(tipo_comp)) && cf == 0.0)
					c.append("E");
				else
					c.append("0");
				if (transaction.equalsIgnoreCase("V"))
					c.append(pad(formatAmount(cf), 15, true));
				c.append(pad(formatAmount(ot), 15, true));
				if (transaction.equalsIgnoreCase("V"))
					// TODO: dar soporte para comisiones de corredores
					c.append("00000000000                              000000000000000");
				if (!transaction.equalsIgnoreCase("V"))
					c.append(fecha);
				c.append(lineSeparator);
 				fw_c.write(c.toString());
 				//fw_c.write("\r");
 				c = null;
 				q_alic = 0;
			}
			
			/*
			 * Guardo el  invoice_id del comprobante por que si en la próxima línea del rs 
			 * el invoice_id tiene el mismo valor entonces tengo que acumular los montos de impuestos.
			 */
			invoice_id = rs.getInt(IX_INVOICE_ID);
			cant++;
		}
	
		return cant;
			
	}
	
	/*
	 * Retorna la fecha en formato anio mes dia
	 */
    private String formatDate(Date fecha) {
        
    	DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        return dateFormat.format(fecha);
    }

    /*
     * Retorna el monto como un string sin el punto decimal 
     */
    private String formatAmount(Double amt) {
    	
    	DecimalFormat df = new DecimalFormat("#.00");
    	return df.format(amt).replace(".", "").replace(",", "");
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
    	cf = 0.0;
    	p_iva = 0.0;
    	p_iibb = 0.0;
    	p_nac = 0.0;
    	p_mun = 0.0;
    	ot = 0.0;
    	ex = 0.00;
    }
    
    /*
     * Devuelve verdadero si el id del impuesto consultado corresponde a uno configurado 
     * como crédito o débto fiscal
     */
    private Boolean esCreditoDebitoFiscal(String reference){
    	return reference.trim().equalsIgnoreCase(LP_C_Tax.CITIRG3685_CréditoODébitoFiscalIVA);
    }
    
    /*
     * Devuelve verdadero si el tipo de comprobante es "Otros comprobantes" u "Otros comprobantes - credito".
     */
    private Boolean esOtros(String tipo){
    	return (tipo.equals("090") || tipo.equals("099"));
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
		if (invoice_id != rs.getInt(IX_INVOICE_ID))
			borraImpuestos();

		if (rs.isLast()) // Si estoy en la última línea, devuelvo true para que se guarde el comprobante.
    		ret = true;
		else{
	    	rs.next();
	    	ret = !(rs.getInt(IX_TAX_CITI_REF) == id);
	    	rs.previous();
		}
		
		String reference = rs.getString(IX_TAX_CITI_REF).trim().toUpperCase();
		if (reference.equals(LP_C_Tax.CITIRG3685_CréditoODébitoFiscalIVA))
			cf +=rs.getDouble(IX_INVOICE_TAX_AMT);
		else if (reference.equals(LP_C_Tax.PERCEPTIONTYPE_IVA))
			p_iva += rs.getDouble(IX_INVOICE_TAX_AMT);
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeIngresosBrutos))
			p_iibb += rs.getDouble(IX_INVOICE_TAX_AMT);
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeImpuestosNacionales))
			p_nac += rs.getDouble(IX_INVOICE_TAX_AMT);
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeImpuestosMunicipales))
			p_mun += rs.getDouble(IX_INVOICE_TAX_AMT);
		else if (reference.equals(LP_C_Tax.CITIRG3685_OtrosImpuestos))
			ot += rs.getDouble(IX_INVOICE_TAX_AMT);
		else if (reference.equals(LP_C_Tax.CITIRG3685_ImportesExentos))
			ex += rs.getDouble(IX_INVOICE_TAX_AMT_BASE);
		
		return ret;
    }
}
