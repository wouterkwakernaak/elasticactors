/*
 * Copyright 2013 - 2017 The Original Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsoftware.elasticactors.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.elasticsoftware.elasticactors.ActorRef;
import org.elasticsoftware.elasticactors.cluster.InternalActorSystem;
import org.elasticsoftware.elasticactors.messaging.ImmutableInternalMessage;
import org.elasticsoftware.elasticactors.messaging.InternalMessage;
import org.elasticsoftware.elasticactors.messaging.InternalMessageImpl;
import org.elasticsoftware.elasticactors.messaging.UUIDTools;
import org.elasticsoftware.elasticactors.serialization.Deserializer;
import org.elasticsoftware.elasticactors.serialization.Message;
import org.elasticsoftware.elasticactors.serialization.protobuf.Elasticactors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.elasticsoftware.elasticactors.messaging.UUIDTools.toUUID;

/**
 * @author Joost van de Wijgerd
 */
public final class InternalMessageDeserializer implements Deserializer<byte[],InternalMessage> {
    private final ActorRefDeserializer actorRefDeserializer;
    private final InternalActorSystem internalActorSystem;

    public InternalMessageDeserializer(ActorRefDeserializer actorRefDeserializer, InternalActorSystem internalActorSystem) {
        this.actorRefDeserializer = actorRefDeserializer;
        this.internalActorSystem = internalActorSystem;
    }

    @Override
    public InternalMessage deserialize(byte[] serializedObject) throws IOException {
        Elasticactors.InternalMessage protobufMessage = Elasticactors.InternalMessage.parseFrom(serializedObject);
        ActorRef sender = (protobufMessage.hasSender()) ? actorRefDeserializer.deserialize(protobufMessage.getSender()) : null;
        // there is either a receiver or a list of receivers
        ImmutableList<ActorRef> receivers;
        ActorRef singleReceiver = (protobufMessage.hasReceiver()) ? actorRefDeserializer.deserialize(protobufMessage.getReceiver()) : null;
        if(singleReceiver == null) {
            ImmutableList.Builder<ActorRef> listBuilder = ImmutableList.builder();
            for (String receiver : protobufMessage.getReceiversList()) {
                listBuilder.add(actorRefDeserializer.deserialize(receiver));
            }
            receivers = listBuilder.build();
        } else {
            receivers = ImmutableList.of(singleReceiver);
        }
        String messageClassString = protobufMessage.getPayloadClass();
        UUID id = toUUID(protobufMessage.getId().toByteArray());
        boolean durable = (!protobufMessage.hasDurable()) || protobufMessage.getDurable();
        boolean undeliverable = protobufMessage.hasUndeliverable() && protobufMessage.getUndeliverable();
        int timeout = protobufMessage.hasTimeout() ? protobufMessage.getTimeout() : InternalMessage.NO_TIMEOUT;
        //return new InternalMessageImpl(id, sender, receivers, protobufMessage.getPayload().asReadOnlyByteBuffer(), messageClassString, durable, undeliverable);
        // optimize immutable message if possible

        Class<?> messageClass = isImmutableMessageClass(messageClassString);
        if(messageClass == null) {
            return new InternalMessageImpl(id, sender, receivers, protobufMessage.getPayload().asReadOnlyByteBuffer(), messageClassString, durable, undeliverable, timeout);
        } else {
            Object payloadObject = internalActorSystem.getDeserializer(messageClass).deserialize(protobufMessage.getPayload().asReadOnlyByteBuffer());
            return new ImmutableInternalMessage(id, sender, receivers, protobufMessage.getPayload().asReadOnlyByteBuffer(), payloadObject, durable, undeliverable, timeout);
        }
    }

    private Class<?> isImmutableMessageClass(String messageClassString) {
        try {
            Class<?> messageClass = Class.forName(messageClassString);
            Message messageAnnotation = messageClass.getAnnotation(Message.class);
            if(messageAnnotation != null &&  messageAnnotation.immutable()) {
                return messageClass;
            } else {
                return null;
            }
        } catch(Exception e) {
            return null;
        }
    }
}
