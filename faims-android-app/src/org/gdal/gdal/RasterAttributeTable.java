/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.4
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.gdal;

public class RasterAttributeTable implements Cloneable {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected RasterAttributeTable(long cPtr, boolean cMemoryOwn) {
    if (cPtr == 0)
        throw new RuntimeException();
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }
  
  protected static long getCPtr(RasterAttributeTable obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        gdalJNI.delete_RasterAttributeTable(swigCPtr);
      }
      swigCPtr = 0;
    }
  }


  public Object clone()
  {
      return Clone();
  }

  public RasterAttributeTable() {
    this(gdalJNI.new_RasterAttributeTable(), true);
  }

  public RasterAttributeTable Clone() {
    long cPtr = gdalJNI.RasterAttributeTable_Clone(swigCPtr, this);
    return (cPtr == 0) ? null : new RasterAttributeTable(cPtr, true);
  }

  public int GetColumnCount() {
    return gdalJNI.RasterAttributeTable_GetColumnCount(swigCPtr, this);
  }

  public String GetNameOfCol(int iCol) {
    return gdalJNI.RasterAttributeTable_GetNameOfCol(swigCPtr, this, iCol);
  }

  public int GetUsageOfCol(int iCol) {
    return gdalJNI.RasterAttributeTable_GetUsageOfCol(swigCPtr, this, iCol);
  }

  public int GetTypeOfCol(int iCol) {
    return gdalJNI.RasterAttributeTable_GetTypeOfCol(swigCPtr, this, iCol);
  }

  public int GetColOfUsage(int eUsage) {
    return gdalJNI.RasterAttributeTable_GetColOfUsage(swigCPtr, this, eUsage);
  }

  public int GetRowCount() {
    return gdalJNI.RasterAttributeTable_GetRowCount(swigCPtr, this);
  }

  public String GetValueAsString(int iRow, int iCol) {
    return gdalJNI.RasterAttributeTable_GetValueAsString(swigCPtr, this, iRow, iCol);
  }

  public int GetValueAsInt(int iRow, int iCol) {
    return gdalJNI.RasterAttributeTable_GetValueAsInt(swigCPtr, this, iRow, iCol);
  }

  public double GetValueAsDouble(int iRow, int iCol) {
    return gdalJNI.RasterAttributeTable_GetValueAsDouble(swigCPtr, this, iRow, iCol);
  }

  public void SetValueAsString(int iRow, int iCol, String pszValue) {
    gdalJNI.RasterAttributeTable_SetValueAsString(swigCPtr, this, iRow, iCol, pszValue);
  }

  public void SetValueAsInt(int iRow, int iCol, int nValue) {
    gdalJNI.RasterAttributeTable_SetValueAsInt(swigCPtr, this, iRow, iCol, nValue);
  }

  public void SetValueAsDouble(int iRow, int iCol, double dfValue) {
    gdalJNI.RasterAttributeTable_SetValueAsDouble(swigCPtr, this, iRow, iCol, dfValue);
  }

  public void SetRowCount(int nCount) {
    gdalJNI.RasterAttributeTable_SetRowCount(swigCPtr, this, nCount);
  }

  public int CreateColumn(String pszName, int eType, int eUsage) {
    return gdalJNI.RasterAttributeTable_CreateColumn(swigCPtr, this, pszName, eType, eUsage);
  }

  public boolean GetLinearBinning(double[] pdfRow0Min, double[] pdfBinSize) {
    return gdalJNI.RasterAttributeTable_GetLinearBinning(swigCPtr, this, pdfRow0Min, pdfBinSize);
  }

  public int SetLinearBinning(double dfRow0Min, double dfBinSize) {
    return gdalJNI.RasterAttributeTable_SetLinearBinning(swigCPtr, this, dfRow0Min, dfBinSize);
  }

  public int GetRowOfValue(double dfValue) {
    return gdalJNI.RasterAttributeTable_GetRowOfValue(swigCPtr, this, dfValue);
  }

}
