/**
 * 
 * Copyright (C) 2014 Seagate Technology.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.seagate.kinetic.simulator.io.provider.nio.udt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.udt.UdtMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.seagate.kinetic.common.lib.KineticMessage;
import com.seagate.kinetic.proto.Kinetic.Command;
import com.seagate.kinetic.proto.Kinetic.Message;
import com.seagate.kinetic.proto.KineticIo.ExtendedMessage;
import com.seagate.kinetic.simulator.io.provider.spi.MessageService;

/**
 * Please note: This class is for evaluation only and in prototype state.
 *
 * @author chiaming
 *
 */
public class UdtRequestProcessRunner implements
Runnable {

    private static final Logger logger = Logger
            .getLogger(UdtRequestProcessRunner.class.getName());

    private MessageService service = null;
    private ChannelHandlerContext ctx = null;
    private byte[] request = null;

    public UdtRequestProcessRunner(MessageService service,
            ChannelHandlerContext ctx, byte[] request) {

        this.service = service;
        this.ctx = ctx;
        this.request = request;
    }

    @Override
    public void run() {
        try {

            // extended request builder
            ExtendedMessage.Builder extendedBuilder = ExtendedMessage
                    .newBuilder().mergeFrom(request);

            // build extended message
            ExtendedMessage extendedMessage = extendedBuilder.build();

            // create kinetic message
            KineticMessage km = new KineticMessage();

            // set interface message
            km.setMessage(extendedMessage.getInterfaceMessage());
            
         // get command bytes
            ByteString commandBytes = extendedMessage.getInterfaceMessage().getCommandBytes();
            
            // build command
            Command.Builder commandBuilder = Command.newBuilder();
            
            try {
                commandBuilder.mergeFrom(commandBytes);
                km.setCommand(commandBuilder.build());
            } catch (InvalidProtocolBufferException e) {
               logger.log(Level.WARNING, e.getMessage(), e);
            }

            // set optiona value
            if (extendedMessage.hasValue()) {
                km.setValue(extendedMessage.getValue().toByteArray());
            }

            // process request
            KineticMessage kmresp = service.processRequest(km);

            // create response builder
            ExtendedMessage.Builder extendedResponse = ExtendedMessage
                    .newBuilder();

            // set response interface message
            extendedResponse.setInterfaceMessage((Message.Builder) kmresp
                    .getMessage());

            // set response value
            if (kmresp.getValue() != null) {
                extendedResponse
                .setValue(ByteString.copyFrom(kmresp.getValue()));
            }

            // get serialized bytes
            byte[] data = extendedResponse.build().toByteArray();

            ByteBuf byteBuf = Unpooled.buffer(data.length);
            byteBuf.writeBytes(data);

            UdtMessage udt = new UdtMessage(byteBuf);

            ctx.writeAndFlush(udt);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

}
