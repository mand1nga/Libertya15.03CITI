package com.jmm.exportaCITIRG3685.callout;

import java.util.Properties;

import org.openXpertya.model.CalloutEngine;
import org.openXpertya.model.MDocType;
import org.openXpertya.model.MField;
import org.openXpertya.model.MLetraComprobante;
import org.openXpertya.model.MTab;
import org.openXpertya.plugin.MPluginStatusCallout;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.Env;

import com.jmm.exportaCITIRG3685.model.MInvoice;

public class CalloutInvoice extends CalloutEngine {
	private static CLogger s_log = CLogger.getCLogger(CalloutInvoice.class);	
		
	public String postC_DocType_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}	
	
	public String postC_DocTypeTarget_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}
	
	public String postC_Letra_Comprobante_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}
	
	private String fixAFIPDocType(Properties ctx, int WindowNo, MTab mTab){
		if(!mTab.isInserting())
			return "";

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
				setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_OtrosComprobantes);
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
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_FacturasA);
						else if(letra.equals("B"))
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_FacturasB);
						else if(letra.equals("C"))
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_FacturasC);						
						else if(letra.equals("M"))
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_FacturasM);
						
					}else if(docBaseType.equals(MDocType.DOCBASETYPE_APCreditMemo)){
						// NCs
						if(letra.equals("A"))
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_NotasDeCreditoA);
						else if(letra.equals("B"))
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_NotasDeCreditoB);
						else if(letra.equals("C"))
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_NotasDeCreditoC);						
						else if(letra.equals("M"))
							setAfipDocType(mTab, MInvoice.AFIPDOCTYPE_NotasDeCreditoM);						
					}
				}
			}
		}else{
			setAfipDocType(mTab, docTypeAFIP);
		}

		return state;
	}
	
	private void setAfipDocType(MTab mTab, String afipDocType){
		if(afipDocType==null)
			return;
		s_log.finest("setting afipdoctype = " + afipDocType);
		try{
			mTab.setValue("afipdoctype", afipDocType);
		}catch(Exception e){
			s_log.warning(e.getMessage());
		}
	}	
}
