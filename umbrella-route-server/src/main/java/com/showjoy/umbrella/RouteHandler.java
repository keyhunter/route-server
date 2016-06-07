package com.showjoy.umbrella;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class RouteHandler extends ChannelInboundHandlerAdapter {

    private HttpRequest request;

    private Logger      log = Logger.getLogger(RouteHandler.class.getName());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttpRequest) {
            request = (DefaultHttpRequest) msg;
            String uri = request.getUri();
            System.out.println("Uri:" + uri);
        }
        HttpHeaders headers = request.headers();
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf buf = content.content();
            System.out.println(buf.toString(io.netty.util.CharsetUtil.UTF_8));
            buf.release();
            FullHttpResponse response = proxyConnect(headers);
            // Write the response.
            ChannelFuture future = ctx.channel().writeAndFlush(response);
            // Close the connection after the write operation is done if necessary.
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private FullHttpResponse proxyConnect(HttpHeaders headers) throws IOException,
                                                               MalformedURLException,
                                                               ProtocolException {
        HttpURLConnection con = (HttpURLConnection) new URL("http://www.showjoy.com")
            .openConnection();
        String methodName = request.getMethod().name();
        con.setRequestMethod(methodName);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setFollowRedirects(false);
        con.setUseCaches(true);
        for (Entry<String, String> entry : headers.entries()) {
            String headerName = entry.getKey();
            con.setRequestProperty(headerName, entry.getValue());
        }
        con.connect();
        InputStream inputStream = con.getInputStream();
        byte[] input2byte = input2byte(inputStream);
        String string = new String(input2byte);
        ByteBuf buffer = Unpooled.copiedBuffer(input2byte);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
            HttpResponseStatus.valueOf(con.getResponseCode()), buffer);

        for (Iterator i = con.getHeaderFields().entrySet().iterator(); i.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) i.next();
            if (mapEntry.getKey() != null)
                response.headers().set(mapEntry.getKey().toString(),
                    ((List) mapEntry.getValue()).get(0).toString());
        }
        return response;
    }

    public final byte[] input2byte(InputStream inStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] in2b = swapStream.toByteArray();
        return in2b;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage());
        ctx.close();
    }

}
