package com.example.administrator.testz.nettydji;


/**
 * Created by wrs on 2019/6/26,11:44
 * projectName: Testz
 * packageName: com.example.administrator.testz
 */


import android.os.Environment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

//wangnetty
public class NettyClientHandler extends SimpleChannelInboundHandler {
    private ChannelHandlerContext context;
    private MessageListener listener;
    private String tenantId;
    private int attempts = 0;


    //wangnetty
    public NettyClientHandler(MessageListener messageListener, NettyClient nettyClinet) {
        this.listener = messageListener;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        System.out.println("channelRead0 service send message: " + o.toString());
        //这是服务端发过来的消息进行接收
    }

    // 建立连接就发送消息
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //第一次连接走的方法
        System.out.println("output connected!!");

        this.context = ctx;
        attempts = 0;
    }

    //断开netty连接
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("offline。。。。。。");
        //使用过程中断线重连
        final EventLoop eventLoop = ctx.channel().eventLoop();
        if (attempts < 12) {
            attempts++;
        }
        int timeout = 2 << attempts;
        eventLoop.schedule(new Runnable() {
            @Override
            public void run() {
                NettyClient.getInstance().start();
            }
        }, timeout, TimeUnit.SECONDS);
        ctx.fireChannelInactive();

    }

    //连接中
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                System.out.println("READER_IDLE");
            } else if (event.state().equals(IdleState.WRITER_IDLE)) {
                /*向服务端发送心跳包,保持长连接*/
                NettyClient.getInstance().sendHeartBeatData("nettyheart");
            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                System.out.println("ALL_IDLE");
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }
}
