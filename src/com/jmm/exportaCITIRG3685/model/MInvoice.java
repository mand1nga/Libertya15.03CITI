/*
 * Extensión de la clase MInvoice para el plugin de exportación de archivos CITI RG3685
 * 
 * Los tipos de documentos definidos por AFIP deben informarse por cada documento. En los informes de ventas
 * no hay inconvenientes, ya que cada tipo de documento (factura, ND, NC) tiene su valor proveniente de DocType.
 * En el caso de las compras, el problema es que hay dos tipos básicos de documento (Factura o abono de proveedor)
 * por lo que es necesario definir el tipo al momento de la carga del comprobante.
 * Si no se especificó ningún valor en el combo, entonces se setea el valor por defecto Facturas A (001).
 * Este valor puede ser cambiado después de haber completado la factura.
 *  
 * Autor: Juan Manuel Martínez - jmmartinezsf@gmail.com
 * Versión 0.1 - septiembre de 2015
 * Para Libertya 15.03
 */

package com.jmm.exportaCITIRG3685.model;
 
import java.util.Properties;
import org.openXpertya.model.PO;
import org.openXpertya.model.MDocType;
import org.openXpertya.plugin.MPluginPO;
import org.openXpertya.plugin.MPluginStatusPO;

public class MInvoice extends MPluginPO {

	public MInvoice(PO po, Properties ctx, String trxName, String aPackage) {
		super(po, ctx, trxName, aPackage);
	}
 
	
	public MPluginStatusPO postBeforeSave(PO po, boolean newRecord) {
		LP_C_Invoice lp_invoice = (LP_C_Invoice) po;
		
		MDocType dt = MDocType.get(po.getCtx(), lp_invoice.getC_DocTypeTarget_ID());

		if (dt.isDocType(MDocType.DOCTYPE_Retencion_Invoice) ||	dt.isDocType(MDocType.DOCTYPE_Retencion_Receipt) ||
			dt.isDocType(MDocType.DOCTYPE_Retencion_InvoiceCustomer) ||	dt.isDocType(MDocType.DOCTYPE_Retencion_ReceiptCustomer))
	
			lp_invoice.setafipdoctype(LP_C_Invoice.AFIPDOCTYPE_OtrosComprobantes);
		else
			// AFIP doctype equivale a "0" + docsubtypecae
			if (lp_invoice.isSOTrx())
				lp_invoice.setafipdoctype(String.format("%1$03d", Integer.parseInt(dt.getdocsubtypecae())));

		
		return status_po;
	}	
}
