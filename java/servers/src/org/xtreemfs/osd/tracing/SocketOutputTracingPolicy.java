/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.tracing;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.OSDRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class SocketOutputTracingPolicy implements TracingPolicy {

    public static final short   POLICY_ID = 6002;

    private static ServerSocket serverSocket = null;
    private static PrintWriter out = null;

    @Override
    public void traceRequest(OSDRequest req) {
        if(out == null) {
            try {
                initSocket();
            } catch (IOException ex) {
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
                return;
            }
        }
        out.write(getTraceString(req) + "\n");
        out.flush();
    }

    private void initSocket() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(9999));
        Socket clientSocket = serverSocket.accept();
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    private String getTraceString(OSDRequest req) {
        // TODO(ckleineweber): extract operation details (offset, length, etc.)
        String result = TimeSync.getGlobalTime() + "," +
                req.getRPCRequest().getConnection().getChannel().socket().getLocalAddress() + ":" + req.getRPCRequest().getConnection().getChannel().socket().getLocalPort() + "," +
                req.getRPCRequest().getSenderAddress().toString() + "," +
                req.getOperation().getClass().toString() + "," +
                req.getFileId();
        return result;
    }
}
