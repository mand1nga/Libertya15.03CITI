package com.jmm.exportaCITIRG3685.callout;

import java.util.Properties;

import org.openXpertya.model.CalloutEngine;
import org.openXpertya.model.MDocType;
import org.openXpertya.model.MField;
import org.openXpertya.model.MInvoice;
import org.openXpertya.model.MLetraComprobante;
import org.openXpertya.model.MTab;
import org.openXpertya.plugin.MPluginStatusCallout;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.Env;

public class CalloutInvoice extends CalloutEngine {
	private static CLogger s_log = CLogger.getCLogger(MInvoice.class);	
	
	public static final String AFIPDOCTYPE_NotasDeDebitoA = "02";
	/** Notas de Credito A = 03 */
	public static final String AFIPDOCTYPE_NotasDeCreditoA = "03";
	/** Recibos A = 04 */
	public static final String AFIPDOCTYPE_RecibosA = "04";
	/** Facturas B = 06 */
	public static final String AFIPDOCTYPE_FacturasB = "06";
	/** Notas de Debito B = 07 */
	public static final String AFIPDOCTYPE_NotasDeDebitoB = "07";
	/** Notas de Credito B = 08 */
	public static final String AFIPDOCTYPE_NotasDeCreditoB = "08";
	/** Recibos B = 09 */
	public static final String AFIPDOCTYPE_RecibosB = "09";
	/** Notas de Venta al contado B = 10 */
	public static final String AFIPDOCTYPE_NotasDeVentaAlContadoB = "10";
	/** Facturas A = 01 */
	public static final String AFIPDOCTYPE_FacturasA = "01";
	/** Notas de Venta al Contado A = 05 */
	public static final String AFIPDOCTYPE_NotasDeVentaAlContadoA = "05";
	/** Facturas C = 11 */
	public static final String AFIPDOCTYPE_FacturasC = "11";
	/** Notas de Debito C = 12 */
	public static final String AFIPDOCTYPE_NotasDeDebitoC = "12";
	/** Notas de Credito C = 13 */
	public static final String AFIPDOCTYPE_NotasDeCreditoC = "13";
	/** Recibos C = 15 */
	public static final String AFIPDOCTYPE_RecibosC = "15";
	/** Liquidación única comercial A = 27 */
	public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialA = "27";
	/** Liquidación única comercial B = 28 */
	public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialB = "28";
	/** Liquidación única comercial C = 29 */
	public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialC = "29";
	/** Liquidación primaria de granos = 33 */
	public static final String AFIPDOCTYPE_LiquidaciónPrimariaDeGranos = "33";
	/** Nota de Crédito Liq. única comercial B = 43 */
	public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialB = "43";
	/** Nota de Crédito Liq. única comercial C = 44 */
	public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialC = "44";
	/** Nota de Débito Liq. única comercial A = 45 */
	public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialA = "45";
	/** Nota de Débito Liq. única comercial B = 46 */
	public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialB = "46";
	/** Nota de Débito Liq. única comercial C = 47 */
	public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialC = "47";
	/** Nota de Crédito Liq. única comercial A = 48 */
	public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialA = "48";
	/** Facturas M = 51 */
	public static final String AFIPDOCTYPE_FacturasM = "51";
	/** Notas de Debito M = 52 */
	public static final String AFIPDOCTYPE_NotasDeDebitoM = "52";
	/** Notas de Credito M = 53 */
	public static final String AFIPDOCTYPE_NotasDeCreditoM = "53";
	/** Tique factura A = 81 */
	public static final String AFIPDOCTYPE_TiqueFacturaA = "81";
	/** Tique factura B = 82 */
	public static final String AFIPDOCTYPE_TiqueFacturaB = "82";
	/** Otros comprobantes - Notas de crédito = 90 */
	public static final String AFIPDOCTYPE_OtrosComprobantes_NotasDeCrédito = "90";
	/** Otros comprobantes = 99 */
	public static final String AFIPDOCTYPE_OtrosComprobantes = "99";
	
	public String postC_DocType_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}	
	
	public String postC_DocTypeTarget_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}
	
	public String postC_Letra_Comprobante_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}
	
	private String fixAFIPDocType(Properties ctx,	int WindowNo, MTab mTab){
		String state = "";
		int cInvoiceId = Env.getContextAsInt(ctx, WindowNo, "C_Invoice_ID");
		boolean isSOTrx = Env.getContext(ctx, WindowNo, "IsSOTrx").equals("Y");		
		int cLetraComprobanteId = Env.getContextAsInt(ctx, WindowNo, "C_Letra_Comprobante_ID");
		int cDocTypeTargetId = Env.getContextAsInt(ctx, WindowNo, "C_DocTypeTarget_ID");
		
		if(cDocTypeTargetId==0)
			return state;		
		
		MDocType dt = MDocType.get(ctx, cDocTypeTargetId);

		String docBaseType = dt.getDocBaseType();
		String docTypeAFIP = dt.getdocsubtypecae();
		
		if(docTypeAFIP==null || docTypeAFIP.isEmpty()){	
			
			if (docBaseType.equals(MDocType.DOCTYPE_Retencion_Invoice) 
					|| docBaseType.equals(MDocType.DOCTYPE_Retencion_Receipt) 
					|| docBaseType.equals(MDocType.DOCTYPE_Retencion_InvoiceCustomer) 
					|| docBaseType.equals(MDocType.DOCTYPE_Retencion_ReceiptCustomer)
			){				
				setAfipDocType(mTab, AFIPDOCTYPE_OtrosComprobantes);
			}else {				
				// Ayuda para determinar el codigo de comprobante segun AFIP
				// Comprobantes de compra, con letra asignada
				if (!isSOTrx && cLetraComprobanteId > 0){

					MLetraComprobante mLetraComprobante = new MLetraComprobante(
							ctx, cLetraComprobanteId, null);

					String letra = mLetraComprobante.getLetra();

					if(docBaseType.equals(MDocType.DOCBASETYPE_APInvoice)){
						// Facturas
						if(letra.equals("A"))
							setAfipDocType(mTab, AFIPDOCTYPE_FacturasA);
						else if(letra.equals("B"))
							setAfipDocType(mTab, AFIPDOCTYPE_FacturasB);
						else if(letra.equals("C"))
							setAfipDocType(mTab, AFIPDOCTYPE_FacturasC);						
						else if(letra.equals("M"))
							setAfipDocType(mTab, AFIPDOCTYPE_FacturasM);
						
					}else if(docBaseType.equals(MDocType.DOCBASETYPE_APCreditMemo)){
						// NCs
						if(letra.equals("A"))
							setAfipDocType(mTab, AFIPDOCTYPE_NotasDeCreditoA);
						else if(letra.equals("B"))
							setAfipDocType(mTab, AFIPDOCTYPE_NotasDeCreditoB);
						else if(letra.equals("C"))
							setAfipDocType(mTab, AFIPDOCTYPE_NotasDeCreditoC);						
						else if(letra.equals("M"))
							setAfipDocType(mTab, AFIPDOCTYPE_NotasDeCreditoM);						
					}
				}
			}
		}else{
			setAfipDocType(mTab, docTypeAFIP);
		}

		return state;
	}
	
	private void setAfipDocType(MTab mTab, String dt){
		s_log.finest("setting afipdoctype = " + dt);
		mTab.setValue("afipdoctype", dt);
	}	
}
