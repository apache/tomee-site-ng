package org.openejb.corba.idl.CosTransactions;


/**
* org/apache/geronimo/interop/CosTransactions/NotPrepared.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from C:/dev/corba/geronimo/trunk/modules/interop/src/idl/CosTransactions.idl
* Saturday, March 12, 2005 1:30:01 PM EST
*/

public final class NotPrepared extends org.omg.CORBA.UserException
{

  public NotPrepared ()
  {
    super(NotPreparedHelper.id());
  } // ctor


  public NotPrepared (String $reason)
  {
    super(NotPreparedHelper.id() + "  " + $reason);
  } // ctor

} // class NotPrepared
