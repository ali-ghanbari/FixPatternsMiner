package org.eclipse.jetty.client;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.eclipse.jetty.http.AbstractGenerator;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.Buffers;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class BlockingHttpConnection extends AbstractHttpConnection
{

    private static final Logger LOG = Log.getLogger(BlockingHttpConnection.class);

    private boolean _requestComplete;
    private int _status;
    private Buffer _requestContentChunk;
    
    BlockingHttpConnection(Buffers requestBuffers, Buffers responseBuffers, EndPoint endp)
    {
        super(requestBuffers,responseBuffers,endp);
    }

    protected void reset() throws IOException
    {
        _requestComplete = false;
        super.reset();
    }
    
    @Override
    public Connection handle() throws IOException
    {
        Connection connection = this;

        try
        {
            boolean failed = false;

            
            // While we are making progress and have not changed connection
            while (_endp.isOpen() && connection==this)
            {
                LOG.debug("open={} more={} buffering={}",_endp.isOpen(),_parser.isMoreInBuffer(),_endp.isBufferingInput());

                HttpExchange exchange;
                synchronized (this)
                {
                    exchange=_exchange;
                    
                    while (exchange == null)
                    {
                        try
                        {
                            this.wait();
                            exchange=_exchange;
                        }
                        catch (InterruptedException e)
                        {
                            throw new InterruptedIOException();
                        }
                    }
                }
                LOG.debug("exchange {}",exchange);
                
                try
                {                  
                    // Should we commit the request?
                    if (!_generator.isCommitted() && exchange!=null && exchange.getStatus() == HttpExchange.STATUS_WAITING_FOR_COMMIT)
                    {
                        LOG.debug("commit");
                        commitRequest();
                    }

                    // Generate output
                    while (_generator.isCommitted() && !_generator.isComplete())
                    {
                        if (_generator.flushBuffer()>0)
                        {
                            LOG.debug("flushed");
                        }

                        // Is there more content to send or should we complete the generator
                        if (_generator.isState(AbstractGenerator.STATE_CONTENT))
                        {
                            // Look for more content to send.
                            if (_requestContentChunk==null)
                                _requestContentChunk = exchange.getRequestContentChunk(null);
                                
                            if (_requestContentChunk==null)
                            {
                                LOG.debug("complete");
                                _generator.complete();
                            }
                            else if (_generator.isEmpty())
                            {
                                LOG.debug("addChunk");
                                Buffer chunk=_requestContentChunk;
                                _requestContentChunk=exchange.getRequestContentChunk(null);
                                _generator.addContent(chunk,_requestContentChunk==null);
                            }
                        }
                    }
                    
                    // Signal request completion
                    if (_generator.isComplete() && !_requestComplete)
                    {
                        LOG.debug("requestComplete");
                        _requestComplete = true;
                        exchange.getEventListener().onRequestComplete();
                    }
                    
                    // Flush output from buffering endpoint
                    if (_endp.isBufferingOutput())
                        _endp.flush();
                    
                    // Read any input that is available
                    if (!_parser.isComplete() && _parser.parseAvailable())
                    {
                        LOG.debug("parsed");
                    }
                    
                }
                catch (Throwable e)
                {
                    LOG.debug("Failure on " + _exchange, e);

                    if (e instanceof ThreadDeath)
                        throw (ThreadDeath)e;

                    failed = true;

                    synchronized (this)
                    {
                        if (exchange != null)
                        {
                            // Cancelling the exchange causes an exception as we close the connection,
                            // but we don't report it as it is normal cancelling operation
                            if (exchange.getStatus() != HttpExchange.STATUS_CANCELLING &&
                                    exchange.getStatus() != HttpExchange.STATUS_CANCELLED &&
                                    !exchange.isDone())
                            {
                                exchange.setStatus(HttpExchange.STATUS_EXCEPTED);
                                exchange.getEventListener().onException(e);
                            }
                        }
                        else
                        {
                            if (e instanceof IOException)
                                throw (IOException)e;
                            if (e instanceof Error)
                                throw (Error)e;
                            if (e instanceof RuntimeException)
                                throw (RuntimeException)e;
                            throw new RuntimeException(e);
                        }
                    }
                }
                finally
                {
                    LOG.debug("{} {}",_generator, _parser);
                    LOG.debug("{}",_endp);
                    
                    boolean complete = failed || _generator.isComplete() && _parser.isComplete();
                    
                    if (complete)
                    {
                        boolean persistent = !failed && _parser.isPersistent() && _generator.isPersistent();
                        _generator.setPersistent(persistent);
                        reset();
                        if (persistent)
                            _endp.setMaxIdleTime((int)_destination.getHttpClient().getIdleTimeout());

                        synchronized (this)
                        {
                            exchange=_exchange;
                            _exchange = null;

                            // Cancel the exchange
                            if (exchange!=null)
                            {
                                exchange.cancelTimeout(_destination.getHttpClient());
                                
                                // TODO should we check the exchange is done?
                            }
                            
                            // handle switched protocols
                            if (_status==HttpStatus.SWITCHING_PROTOCOLS_101)
                            {
                                Connection switched=exchange.onSwitchProtocol(_endp);
                                if (switched!=null)
                                    connection=switched;
                                {
                                    // switched protocol!
                                    _pipeline = null;
                                    if (_pipeline!=null)
                                        _destination.send(_pipeline);
                                    _pipeline = null;

                                    connection=switched;
                                }
                            }
                            
                            // handle pipelined requests
                            if (_pipeline!=null)
                            {
                                if (!persistent || connection!=this)
                                    _destination.send(_pipeline);
                                else
                                    _exchange=_pipeline;
                                _pipeline=null;
                            }
                            
                            if (_exchange==null && !isReserved())  // TODO how do we return switched connections?
                                _destination.returnConnection(this, !persistent);
                        }
                    
                    }
                }
            }
        }
        finally
        {
            _parser.returnBuffers();
            _generator.returnBuffers();
        }

        return connection;
    }

    
    public void onInputShutdown() throws IOException
    {
        if (_generator.isIdle())
            _endp.shutdownOutput();
    }

    
    @Override
    public boolean send(HttpExchange ex) throws IOException
    {
        boolean sent=super.send(ex);
        if (sent)
        {
            synchronized (this)
            {
                notifyAll();
            }
        }
        return sent;
    }
}
