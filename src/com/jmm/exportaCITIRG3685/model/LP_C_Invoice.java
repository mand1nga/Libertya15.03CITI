/** Modelo Generado - NO CAMBIAR MANUALMENTE - Disytel */
package com.jmm.exportaCITIRG3685.model;
import org.openXpertya.model.*;
import java.util.logging.Level;
 import java.util.*;
import java.sql.*;
import java.math.*;
import org.openXpertya.util.*;
/** Modelo Generado por C_Invoice
 *  @author Comunidad de Desarrollo Libertya*         *Basado en Codigo Original Modificado, Revisado y Optimizado de:*         * Jorg Janke 
 *  @version  - 2015-09-30 17:22:02.311 */
public class LP_C_Invoice extends org.openXpertya.model.MInvoice
{
/** Constructor estándar */
public LP_C_Invoice (Properties ctx, int C_Invoice_ID, String trxName)
{
super (ctx, C_Invoice_ID, trxName);
/** if (C_Invoice_ID == 0)
{
}
 */
}
/** Load Constructor */
public LP_C_Invoice (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
public String toString()
{
StringBuffer sb = new StringBuffer ("LP_C_Invoice[").append(getID()).append("]");
return sb.toString();
}
public static final int AFIPDOCTYPE_AD_Reference_ID = MReference.getReferenceID("DocSubTypeCae");
/** Notas de Debito A = 002 */
public static final String AFIPDOCTYPE_NotasDeDebitoA = "002";
/** Notas de Credito A = 003 */
public static final String AFIPDOCTYPE_NotasDeCreditoA = "003";
/** Recibos A = 004 */
public static final String AFIPDOCTYPE_RecibosA = "004";
/** Facturas B = 006 */
public static final String AFIPDOCTYPE_FacturasB = "006";
/** Notas de Debito B = 007 */
public static final String AFIPDOCTYPE_NotasDeDebitoB = "007";
/** Notas de Credito B = 008 */
public static final String AFIPDOCTYPE_NotasDeCreditoB = "008";
/** Recibos B = 009 */
public static final String AFIPDOCTYPE_RecibosB = "009";
/** Notas de Venta al contado B = 010 */
public static final String AFIPDOCTYPE_NotasDeVentaAlContadoB = "010";
/** Facturas A = 001 */
public static final String AFIPDOCTYPE_FacturasA = "001";
/** Notas de Venta al Contado A = 005 */
public static final String AFIPDOCTYPE_NotasDeVentaAlContadoA = "005";
/** Facturas C = 011 */
public static final String AFIPDOCTYPE_FacturasC = "011";
/** Notas de Debito C = 012 */
public static final String AFIPDOCTYPE_NotasDeDebitoC = "012";
/** Notas de Credito C = 013 */
public static final String AFIPDOCTYPE_NotasDeCreditoC = "013";
/** Recibos C = 015 */
public static final String AFIPDOCTYPE_RecibosC = "015";
/** Liquidación única comercial A = 027 */
public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialA = "027";
/** Liquidación única comercial B = 028 */
public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialB = "028";
/** Liquidación única comercial C = 029 */
public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialC = "029";
/** Liquidación primaria de granos = 033 */
public static final String AFIPDOCTYPE_LiquidaciónPrimariaDeGranos = "033";
/** Nota de Crédito Liq. única comercial B = 043 */
public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialB = "043";
/** Nota de Crédito Liq. única comercial C = 044 */
public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialC = "044";
/** Nota de Débito Liq. única comercial A = 045 */
public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialA = "045";
/** Nota de Débito Liq. única comercial B = 046 */
public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialB = "046";
/** Nota de Débito Liq. única comercial C = 047 */
public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialC = "047";
/** Nota de Crédito Liq. única comercial A = 048 */
public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialA = "048";
/** Facturas M = 051 */
public static final String AFIPDOCTYPE_FacturasM = "051";
/** Notas de Debito M = 052 */
public static final String AFIPDOCTYPE_NotasDeDebitoM = "052";
/** Notas de Credito M = 053 */
public static final String AFIPDOCTYPE_NotasDeCreditoM = "053";
/** Tique factura A = 081 */
public static final String AFIPDOCTYPE_TiqueFacturaA = "081";
/** Tique factura B = 082 */
public static final String AFIPDOCTYPE_TiqueFacturaB = "082";
/** Otros comprobantes - Notas de crédito = 090 */
public static final String AFIPDOCTYPE_OtrosComprobantes_NotasDeCrédito = "090";
/** Otros comprobantes = 099 */
public static final String AFIPDOCTYPE_OtrosComprobantes = "099";
/** Set Tipo documento CITI */
public void setafipdoctype (String afipdoctype)
{
if (afipdoctype == null || afipdoctype.equals("002") || afipdoctype.equals("003") || afipdoctype.equals("004") || afipdoctype.equals("006") || afipdoctype.equals("007") || afipdoctype.equals("008") || afipdoctype.equals("009") || afipdoctype.equals("010") || afipdoctype.equals("001") || afipdoctype.equals("005") || afipdoctype.equals("011") || afipdoctype.equals("012") || afipdoctype.equals("013") || afipdoctype.equals("015") || afipdoctype.equals("027") || afipdoctype.equals("028") || afipdoctype.equals("029") || afipdoctype.equals("033") || afipdoctype.equals("043") || afipdoctype.equals("044") || afipdoctype.equals("045") || afipdoctype.equals("046") || afipdoctype.equals("047") || afipdoctype.equals("048") || afipdoctype.equals("051") || afipdoctype.equals("052") || afipdoctype.equals("053") || afipdoctype.equals("081") || afipdoctype.equals("082") || afipdoctype.equals("090") || afipdoctype.equals("099"));
 else throw new IllegalArgumentException ("afipdoctype Invalid value - Reference = AFIPDOCTYPE_AD_Reference_ID - 002 - 003 - 004 - 006 - 007 - 008 - 009 - 010 - 001 - 005 - 011 - 012 - 013 - 015 - 027 - 028 - 029 - 033 - 043 - 044 - 045 - 046 - 047 - 048 - 051 - 052 - 053 - 081 - 082 - 090 - 099");
if (afipdoctype != null && afipdoctype.length() > 3)
{
log.warning("Length > 3 - truncated");
afipdoctype = afipdoctype.substring(0,3);
}
set_Value ("afipdoctype", afipdoctype);
}
/** Get Tipo documento CITI */
public String getafipdoctype() 
{
return (String)get_Value("afipdoctype");
}
}
