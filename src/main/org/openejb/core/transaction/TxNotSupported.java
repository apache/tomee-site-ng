package org.openejb.core.transaction;

import java.rmi.RemoteException;
import javax.ejb.EnterpriseBean;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRolledbackException;
import org.openejb.ApplicationException;
import org.openejb.OpenEJB;
import org.openejb.SystemException;
import org.openejb.core.ThreadContext;

/**
 * 17.6.2.1 NotSupported
 * 
 * The Container invokes an enterprise Bean method whose transaction attribute
 * is set to NotSupported with an unspecified transaction context.
 * 
 * If a client calls with a transaction context, the container suspends the 
 * association of the transaction context with the current thread before 
 * invoking the enterprise bean's business method. The container resumes the 
 * suspended association when the business method has completed. The suspended
 * transaction context of the client is not passed to the resource managers or
 * other enterprise Bean objects that are invoked from the business method.
 * 
 * If the business method invokes other enterprise beans, the Container passes 
 * no transaction context with the invocation.
 * 
 * Refer to Subsection 17.6.5 for more details of how the Container can 
 * implement this case.
 *
 * @author <a href="mailto=david.blevins@visi.com">David Blevins</a>
 * @version $Revision$ $Date$
 */
public class TxNotSupported extends TransactionPolicy {
    
    public TxNotSupported(TransactionContainer container){
        this();
        this.container = container;
    }

    public TxNotSupported(){
        policyType = NotSupported;
    }
    
    public String policyToString() {
        return "TX_NotSupported: ";
    }

    public void beforeInvoke(EnterpriseBean instance, TransactionContext context) throws org.openejb.SystemException, org.openejb.ApplicationException{
        
        try {
            // if no transaction ---> suspend returns null
            context.clientTx = getTxMngr().suspend();
        } catch ( javax.transaction.SystemException se ) {
            throw new org.openejb.SystemException(se);
        }
        context.currentTx = null;
    
    }

    public void afterInvoke(EnterpriseBean instance, TransactionContext context) throws org.openejb.ApplicationException, org.openejb.SystemException{

        if ( context.clientTx != null ) {
            try{
                getTxMngr( ).resume( context.clientTx );
            }catch(javax.transaction.InvalidTransactionException ite){
                // TODO:3: Localize the message; add to Messages.java
                logger.error("Could not resume the client's transaction, the transaction is no longer valid: "+ite.getMessage());
            }catch(IllegalStateException e){
                // TODO:3: Localize the message; add to Messages.java
                logger.error("Could not resume the client's transaction: "+e.getMessage());
            }catch(javax.transaction.SystemException e){
                // TODO:3: Localize the message; add to Messages.java
                logger.error("Could not resume the client's transaction: The transaction reported a system exception: "+e.getMessage());
            }
        }
    }

    
    /**
     * <B>Container's action</B>
     * 
     * <P>
     * Re-throw AppException
     * </P>
     * 
     * <B>Client's view</B>
     * 
     * <P>
     * Receives AppException.
     * 
     * If the client executes in a transaction, the client's transaction is not 
     * marked for rollback, and client can continue its work.
     * </P>
     */
    public void handleApplicationException( Throwable appException, TransactionContext context) throws ApplicationException{
        // Re-throw AppException
        throw new ApplicationException( appException );
    }
    
    /**
     * A system exception is any exception that is not an Application Exception.
     * 
     * <B>Container's action</B>
     * 
     * <P>
     * <OL>
     * <LI>
     * Log the exception or error so that the System Administrator is alerted of
     * the problem.
     * </LI>
     * <LI>
     * Discard instance.  The Container must not invoke any business methods or
     * container callbacks on the instance.
     * </LI>
     * <LI>
     * Throw RemoteException to remote client;
     * throw EJBException to local client.
     * </LI>
     * </OL>
     * 
     * </P>
     * 
     * <B>Client's view</B>
     * 
     * <P>
     * Receives RemoteException or EJBException.
     * 
     * If the client executes in a transaction, the client's transaction may or
     * may not be marked for rollback.
     * </P>
     */
    public void handleSystemException( Throwable sysException, EnterpriseBean instance, TransactionContext context) throws org.openejb.ApplicationException, org.openejb.SystemException{
        /* [1] Log the system exception or error *********/
        logSystemException( sysException );

        /* [2] Discard instance. *************************/
        discardBeanInstance( instance, context.callContext);

        /* [3] Throw RemoteException to client ***********/
        throwExceptionToServer( sysException );
    }

}

