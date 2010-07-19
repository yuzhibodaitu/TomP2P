/*
 * Copyright 2009 Thomas Bocek
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.tomp2p.message;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * The message is in binary format in TomP2P. It is defined as follows and has
 * several header and payload fields. Since we do the serialization manually, we
 * do not need a serialization field.
 * 
 * @author Thomas Bocek
 * 
 */
public class Message
{
	// used for creating random message id
	final transient private static Random random = new Random();
	// 2 x 4 bit -> 8 bit
	public enum Content
	{
		EMPTY, KEY, KEY_KEY, MAP_KEY_DATA, MAP_KEY_KEY, SET_KEYS, SET_NEIGHBORS, CHANNEL_BUFFER, LONG, INTEGER, MAP_PEER_DATA, PUBLIC_KEY_SIGNATURE, PUBLIC_KEY, RESERVED1, RESERVED2, RESERVED3
	};
	// 1 x 4 bit
	public enum Type
	{
		// REQUEST_1 is the normal request
		// REQUEST_2 for PUT/MOVE/COPY means put if absent
		// REQUEST_3 for ADD/PUT means protect domain
		// REQUEST_4 for PUT means protect domain and put if absent
		// REQUEST_2 for REMOVE means send back results
		// REQUEST_2 for RAW_DATA means serilazie object
		// *** NEIGHBORS has four different cases
		// REQUEST_1 for NEIGHBORS_* means check for get (withDigest)
		// REQUEST_2 for NEIGHBORS_* means check for put (no digest)
		REQUEST_1, REQUEST_2, REQUEST_3, REQUEST_4, OK, PARTIALLY_OK, NOT_FOUND, DENIED, UNKNOWN_ID, EXCEPTION, CANCEL, USER1, USER2, USER3, USER4
	};
	// 1 x 4 bit
	public enum Command
	{
		PING, PUT, GET, ADD, REMOVE, SYNC, NEIGHBORS_STORAGE, NEIGHBORS_TRACKER, QUIT, DIRECT_DATA, TRACKER_ADD, TRACKER_GET, USER1, USER2, USER3, USER4
	};
	// header
	private int messageId;
	private int version;
	private Type type;
	private Command command;
	private PeerAddress sender;
	private PeerAddress realSender;
	private PeerAddress recipient;
	private int contentLength = 0;
	// payload
	private Collection<PeerAddress> neighbors = null;
	private int useAtMostNeighbors = -1;
	private Map<Number160, Data> dataMap = null;
	private Number160 key1 = null;
	private Number160 key2 = null;
	private Number160 key3 = null;
	private Map<Number160, Number160> keyMap = null;
	private Map<PeerAddress, Data> peerDataMap = null;
	private Collection<Number160> keys = null;
	private ChannelBuffer payload = null;
	private long long_number = 0;
	private int int_number = 0;
	private Content contentType1 = Content.EMPTY;
	private Content contentType2 = Content.EMPTY;
	private Content contentType3 = Content.EMPTY;
	private Content contentType4 = Content.EMPTY;
	private PublicKey publicKey = null;
	//
	private volatile transient long finished = 0;
	private volatile transient boolean isUDP=true;
	private volatile transient PrivateKey privateKey;
	private volatile transient boolean hintDataPublickKey = false;
	private volatile transient boolean hintSign = false;

	// final private transient KeyPair keyPair;
	// private volatile transient boolean sign=false;
	/**
	 * Creates message with a random ID
	 */
	public Message()
	{
		setMessageId(random.nextInt());
	}

	// KeyPair getKeyPair()
	// {
	// return keyPair;
	// }
	// public void setKeyPair(KeyPair keyPair)
	// {
	// this.keyPair=keyPair;
	// }
	// public void setSign(boolean sign)
	// {
	// this.sign=sign;
	// }
	// public boolean getSign()
	// {
	// return sign;
	// }
	/**
	 * Randomly generated message ID
	 * 
	 * @return message Id
	 */
	public int getMessageId()
	{
		return messageId;
	}

	/**
	 * For deserialization, we need to set the id
	 * 
	 * @param messageId The message Id
	 */
	public Message setMessageId(final int messageId)
	{
		this.messageId = messageId;
		return this;
	}
	
	public void setUDP()
	{
		isUDP=true;
	}
	
	public void setTCP()
	{
		isUDP=false;
	}
	
	public boolean isUDP()
	{
		return isUDP;
	}

	public void finished()
	{
		this.finished = System.currentTimeMillis();
	}

	public long getFinished()
	{
		if (finished == 0)
			throw new RuntimeException("Need to set finished before!");
		return finished;
	}

	/**
	 * Returns the version, which is 32bit. Each application can choose and
	 * version to not intefere with other applications
	 * 
	 * @return The application version that uses this P2P framework
	 */
	public int getVersion()
	{
		return version;
	}

	/**
	 * For deserialization
	 * 
	 * @param version The 24bit version
	 */
	public Message setVersion(final int version)
	{
		this.version = version;
		return this;
	}

	/**
	 * Determines if its a request oCommandr reply, and what kind of reply
	 * (error, warning states)
	 * 
	 * @return Type of the message
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Set the message type. Either its a request or reply (with error and
	 * warning codes)
	 * 
	 * @param type Type of the message
	 */
	public Message setType(final Type type)
	{
		this.type = type;
		return this;
	}

	/**
	 * Command of the message, such as GET, PING, etc.
	 * 
	 * @return Command
	 */
	public Command getCommand()
	{
		return command;
	}

	/**
	 * Command of the message, such as GET, PING, etc.
	 * 
	 * @param command Command
	 */
	public Message setCommand(final Command command)
	{
		this.command = command;
		return this;
	}

	/**
	 * The ID of the real sender as seen by us.
	 * 
	 * @returnThe ID of the sender.
	 */
	public PeerAddress getRealSender()
	{
		return realSender;
	}

	/**
	 * The ID of the real sender as seen by us.
	 * 
	 * @param sender The ID of the sender.
	 */
	public Message setRealSender(final PeerAddress realSender)
	{
		this.realSender = realSender;
		return this;
	}

	/**
	 * The ID of the sender. Note that the IP is set via the socket.
	 * 
	 * @returnThe ID of the sender.
	 */
	public PeerAddress getSender()
	{
		return sender;
	}

	/**
	 * The ID of the sender. The IP of the sender will *not* be transferred, as
	 * this information is in the IP packet.
	 * 
	 * @param sender The ID of the sender.
	 */
	public Message setSender(final PeerAddress sender)
	{
		this.sender = sender;
		return this;
	}

	/**
	 * The ID of the recipient. Note that the IP is set via the socket.
	 * 
	 * @return The ID of the recipient
	 */
	public PeerAddress getRecipient()
	{
		return recipient;
	}

	/**
	 * Set the ID of the recipient. The IP is used to connect to the recipient,
	 * but the IP is *not* transferred.
	 * 
	 * @param recipient The ID of the recipient
	 */
	public Message setRecipient(final PeerAddress recipient)
	{
		this.recipient = recipient;
		return this;
	}

	/**
	 * The length of the payload
	 * 
	 * @return Length of the payload, if no payload set, returns 0.
	 */
	public int getContentLength()
	{
		return contentLength;
	}

	/**
	 * Set payload length. This can also be used to not transfer payload even if
	 * payload has been set. If contentlength is set to 0, no payload will be
	 * transferred.
	 * 
	 * @param contentLength The length of the payload
	 */
	public Message setContentLength(final int contentLength)
	{
		this.contentLength = contentLength;
		return this;
	}

	/**
	 * Set two content types. The contentypes itself could also be combined. As
	 * not all combinations are used, these two fields are engouh.
	 * 
	 * @param contentType1 Content type 1
	 * @param contentType2 Content type 2
	 */
	void setContentType(final Content contentType1, final Content contentType2,
			final Content contentType3, final Content contentType4)
	{
		this.contentType1 = contentType1;
		this.contentType2 = contentType2;
		this.contentType3 = contentType3;
		this.contentType4 = contentType4;
	}

	/**
	 * Returns first content type. Content type can be empty if not set
	 * 
	 * @return Content type 1
	 */
	public Content getContentType1()
	{
		return contentType1;
	}

	/**
	 * Returns second content type, only if first is not empty
	 * 
	 * @return Content type 2
	 */
	public Content getContentType2()
	{
		return contentType2;
	}

	/**
	 * Returns second content type, only if first is not empty
	 * 
	 * @return Content type 2
	 */
	public Content getContentType3()
	{
		return contentType3;
	}

	/**
	 * Returns second content type, only if first is not empty
	 * 
	 * @return Content type 2
	 */
	public Content getContentType4()
	{
		return contentType4;
	}

	/**
	 * Convient method to set content type. Set first content type 1, if this is
	 * set (not empty), then set the second one.
	 * 
	 * @param contentType
	 */
	public Message setContentType(final Content contentType)
	{
		if (contentType1 == Content.EMPTY)
			contentType1 = contentType;
		else if (contentType2 == Content.EMPTY)
			contentType2 = contentType;
		else if (contentType3 == Content.EMPTY)
			contentType3 = contentType;
		else if (contentType4 == Content.EMPTY)
			contentType4 = contentType;
		else
			throw new IllegalArgumentException(
					"Both content types already set. Cannot set content type!");
		return this;
	}

	public boolean isRequest()
	{
		return type == Type.REQUEST_1 || type == Type.REQUEST_2 || type == Type.REQUEST_3
				|| type == Type.REQUEST_4;
	}

	public boolean isOk()
	{
		return type == Type.OK || type == Type.PARTIALLY_OK;
	}

	public boolean isNotOk()
	{
		return type == Type.NOT_FOUND || type == Type.DENIED;
	}

	public boolean isError()
	{
		return type == Type.UNKNOWN_ID || type == Type.EXCEPTION || type == Type.CANCEL;
	}

	// Here begins the payload part
	public Message setNeighbors(final Collection<PeerAddress> neighbors)
	{
		return setNeighbors(neighbors, neighbors.size());
	}

	public Message setNeighbors(final Collection<PeerAddress> neighbors,
			final int useAtMostNeighbors)
	{
		if (neighbors == null)
			throw new IllegalArgumentException("neighbors cannot add null");
		else if (useAtMostNeighbors < 0)
			throw new IllegalArgumentException("neigbor size is negative");
		this.neighbors = neighbors;
		this.useAtMostNeighbors = useAtMostNeighbors;
		setContentType(Content.SET_NEIGHBORS);
		return this;
	}

	void setNeighbors0(final Collection<PeerAddress> neighbors)
	{
		this.neighbors = neighbors;
		this.useAtMostNeighbors = -1;
	}

	/**
	 * Returns the stored neighbors
	 * 
	 * @return Null if no neighbors set or the list of neighbors
	 */
	public Collection<PeerAddress> getNeighbors()
	{
		return neighbors;
	}

	int getUseAtMostNeighbors()
	{
		return useAtMostNeighbors;
	}

	public Message setKeys(final Collection<Number160> keys)
	{
		if (keys == null)
			throw new IllegalArgumentException("key cannot add null");
		this.keys = keys;
		setContentType(Content.SET_KEYS);
		return this;
	}

	void setKeys0(final Collection<Number160> keys)
	{
		this.keys = keys;
	}

	public Collection<Number160> getKeys()
	{
		return keys;
	}

	// /////////////////////////////////////////////
	public Message setDataMap(final Map<Number160, Data> dataMap)
	{
		if (dataMap == null)
			throw new IllegalArgumentException("key cannot add null");
		this.dataMap = dataMap;
		setContentType(Content.MAP_KEY_DATA);
		return this;
	}

	void setDataMap0(final Map<Number160, Data> dataMap)
	{
		this.dataMap = dataMap;
	}

	public Map<Number160, Data> getDataMap()
	{
		return dataMap;
	}

	// /////////////////////////////////////////////
	public Message setKey(final Number160 key3)
	{
		if (key3 == null)
			throw new IllegalArgumentException("key cannot add null");
		this.key3 = key3;
		setContentType(Content.KEY);
		return this;
	}

	void setKey0(final Number160 key3)
	{
		this.key3 = key3;
	}

	public Message setKeyKey(final Number160 key1, final Number160 key2)
	{
		if (key1 == null || key2 == null)
			throw new IllegalArgumentException("key cannot add null");
		this.key1 = key1;
		this.key2 = key2;
		setContentType(Content.KEY_KEY);
		return this;
	}

	void setKeyKey0(final Number160 key1, final Number160 key2)
	{
		this.key1 = key1;
		this.key2 = key2;
	}

	public Number160 getKey1()
	{
		return key1;
	}

	public Number160 getKey2()
	{
		return key2;
	}

	public Number160 getKey3()
	{
		return key3;
	}

	// /////////////////////////////////////////////
	public Message setKeyMap(final Map<Number160, Number160> keyMap)
	{
		if (keyMap == null)
			throw new IllegalArgumentException("key cannot add null");
		this.keyMap = keyMap;
		setContentType(Content.MAP_KEY_KEY);
		return this;
	}

	void setKeyMap0(final Map<Number160, Number160> keyMap)
	{
		this.keyMap = keyMap;
	}

	public Map<Number160, Number160> getKeyMap()
	{
		return keyMap;
	}

	// /////////////////////////////////////////////
	public Message setLong(final long long_number)
	{
		this.long_number = long_number;
		setContentType(Content.LONG);
		return this;
	}

	void setLong0(final long long_number)
	{
		this.long_number = long_number;
	}

	public long getLong()
	{
		return long_number;
	}

	// /////////////////////////////////////////////
	public Message setPayload(final ChannelBuffer payload)
	{
		if (payload == null)
			throw new RuntimeException("payload cannot add null");
		this.payload = payload;
		setContentType(Content.CHANNEL_BUFFER);
		return this;
	}

	void setPayload0(final ChannelBuffer payload)
	{
		this.payload = payload;
	}

	public ChannelBuffer getPayload()
	{
		return payload;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("Message: id=");
		sb.append(getMessageId());
		sb.append(",c=").append(getCommand().toString()).append(",t=").append(type.toString())
				.append(",l=").append(getContentLength()).append(",s=").append(getSender()).append(
						",r=").append(getRecipient()).append(",k=").append(keys);
		return sb.toString();
	}

	// ///////////////////////////////
	public Message setInteger(final int int_number)
	{
		this.int_number = int_number;
		setContentType(Content.INTEGER);
		return this;
	}

	void setInteger0(final int int_number)
	{
		this.int_number = int_number;
	}

	public int getInteger()
	{
		return int_number;
	}

	// ///////////////////////////////
	public Map<PeerAddress, Data> getPeerDataMap()
	{
		return peerDataMap;
	}

	void setPeerDataMap0(Map<PeerAddress, Data> peerDataMap)
	{
		this.peerDataMap = peerDataMap;
	}

	public Message setPeerDataMap(Map<PeerAddress, Data> peerDataMap)
	{
		if (peerDataMap == null)
			throw new RuntimeException("peerDataMap cannot add null");
		this.peerDataMap = peerDataMap;
		setContentType(Content.MAP_PEER_DATA);
		return this;
	}

	// for internal use only
	void setPublicKey0(PublicKey publicKey)
	{
		this.publicKey = publicKey;
	}

	public PublicKey getPublicKey()
	{
		return publicKey;
	}

	PrivateKey getPrivateKey()
	{
		return privateKey;
	}

	public Message setPublicKey(PublicKey publicKey)
	{
		setContentType(Content.PUBLIC_KEY_SIGNATURE);
		this.publicKey = publicKey;
		return this;
	}

	public Message setPublicKeyAndSign(KeyPair keyPair)
	{
		setContentType(Content.PUBLIC_KEY_SIGNATURE);
		this.publicKey = keyPair.getPublic();
		this.privateKey = keyPair.getPrivate();
		return this;
	}

	boolean isHintDataPublickKey()
	{
		return hintDataPublickKey;
	}

	void setHintDataPublickKey(boolean hintDataPublickKey)
	{
		this.hintDataPublickKey = hintDataPublickKey;
	}

	public void setHintSign(boolean hintSign)
	{
		this.hintSign = hintSign;
	}

	public boolean isHintSign()
	{
		return hintSign;
	}
}