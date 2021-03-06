/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.kernel.ha;

import org.neo5j.com.Deserializer;
import org.neo5j.com.ObjectSerializer;
import org.neo5j.com.Protocol;
import org.neo5j.com.Protocol320;
import org.neo5j.com.ProtocolVersion;
import org.neo5j.com.monitor.RequestMonitor;
import org.neo5j.com.storecopy.ResponseUnpacker;
import org.neo5j.kernel.ha.lock.LockResult;
import org.neo5j.kernel.ha.lock.LockStatus;
import org.neo5j.kernel.impl.store.StoreId;
import org.neo5j.kernel.impl.transaction.log.ReadableClosablePositionAwareChannel;
import org.neo5j.kernel.impl.transaction.log.entry.LogEntryReader;
import org.neo5j.kernel.monitoring.ByteCounterMonitor;
import org.neo5j.logging.LogProvider;

import static org.neo5j.com.Protocol.readString;
import static org.neo5j.com.Protocol.writeString;
import static org.neo5j.com.ProtocolVersion.INTERNAL_PROTOCOL_VERSION;

public class MasterClient320 extends MasterClient310
{
    /* Version 1 first version
     * Version 2 since 2012-01-24
     * Version 3 since 2012-02-16
     * Version 4 since 2012-07-05
     * Version 5 since ?
     * Version 6 since 2014-01-07
     * Version 7 since 2014-03-18
     * Version 8 since 2014-08-27
     * Version 9 since 3.1.0, 2016-09-20
     * Version 10 since 3.2.0, 2016-12-07
     */
    public static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion( (byte) 10, INTERNAL_PROTOCOL_VERSION );

    // From 3.2.0 and onwards, LockResult messages can have messages, or not, independently of their LockStatus.
    public static final ObjectSerializer<LockResult> LOCK_RESULT_OBJECT_SERIALIZER = ( responseObject, result ) ->
    {
        result.writeByte( responseObject.getStatus().ordinal() );
        String message = responseObject.getMessage();
        if ( message != null )
        {
            writeString( result, message );
        }
        else
        {
            result.writeInt( 0 );
        }
    };

    public static final Deserializer<LockResult> LOCK_RESULT_DESERIALIZER = ( buffer, temporaryBuffer ) ->
    {
        byte statusOrdinal = buffer.readByte();
        int messageLength = buffer.readInt();
        LockStatus status;
        try
        {
            status = LockStatus.values()[statusOrdinal];
        }
        catch ( ArrayIndexOutOfBoundsException e )
        {
            throw withInvalidOrdinalMessage( buffer, statusOrdinal, e );
        }
        if ( messageLength > 0 )
        {
            return new LockResult( status, readString( buffer, messageLength ) );
        }
        else
        {
            return new LockResult( status );
        }
    };

    public MasterClient320( String destinationHostNameOrIp, int destinationPort, String originHostNameOrIp,
                            LogProvider logProvider, StoreId storeId,
                            long readTimeoutMillis, long lockReadTimeout, int maxConcurrentChannels, int chunkSize,
                            ResponseUnpacker unpacker,
                            ByteCounterMonitor byteCounterMonitor,
                            RequestMonitor requestMonitor,
                            LogEntryReader<ReadableClosablePositionAwareChannel> entryReader )
    {
        super( destinationHostNameOrIp, destinationPort, originHostNameOrIp, logProvider, storeId, readTimeoutMillis,
                lockReadTimeout, maxConcurrentChannels, chunkSize, unpacker, byteCounterMonitor, requestMonitor,
                entryReader );
    }

    @Override
    protected Protocol createProtocol( int chunkSize, byte applicationProtocolVersion )
    {
        return new Protocol320( chunkSize, applicationProtocolVersion, getInternalProtocolVersion() );
    }

    @Override
    public ObjectSerializer<LockResult> createLockResultSerializer()
    {
        return LOCK_RESULT_OBJECT_SERIALIZER;
    }

    @Override
    public Deserializer<LockResult> createLockResultDeserializer()
    {
        return LOCK_RESULT_DESERIALIZER;
    }

    @Override
    public ProtocolVersion getProtocolVersion()
    {
        return PROTOCOL_VERSION;
    }
}
