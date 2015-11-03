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

}
