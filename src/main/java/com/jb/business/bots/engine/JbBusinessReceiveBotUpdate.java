package com.jb.business.bots.engine;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jb.entities.JbEntityBotUpdateId;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * Handles an update pushed by the instant messenger, which reaches the bot through the webhook instead of the reading of
 * {@code JbInstantMessengerMessageReader}: it leaves the update in the same simplified form that reader produces and hands
 * it to the bot. The two ways share the offset of the bot in {@link JbEntityBotUpdateId}.
 * <p>
 * The offset is saved <b>before</b> the bot runs: Telegram delivers an update again when the answer takes too long or is
 * not a 2xx, and an update must not run twice (it may approve a ticket) nor, by failing, hold the updates behind it, since
 * the webhook is registered with one connection, which delivers them one at a time, in order. For the same reason a
 * delivery repeated is always that of the last update received, so it is recognized by equality. A comparison of order
 * would not do: after a week without updates Telegram picks the next id at random, and a lower one would be ignored
 * forever.
 */
public class JbBusinessReceiveBotUpdate implements CcpBusiness {

	/** Fields of the Telegram update and of the simplified message. */
	public static enum Fields implements CcpJsonFieldName{
		/** The id of the update. */
		update_id,
		/** The chat of the message. */
		chat,
		/** The sender of the message. */
		from,
		/** The user name of the sender. */
		username,
		/** The id of the update, in the simplified message. */
		updateId,
		/** The user name, in the simplified message. */
		userName,
		/** When the message was sent, in the simplified message. */
		sentAt
	}

	/** The single instance. */
	public static final JbBusinessReceiveBotUpdate INSTANCE = new JbBusinessReceiveBotUpdate();

	/** The offset before any update was received. */
	private static final Long FIRST_OFFSET = 0L;

	private JbBusinessReceiveBotUpdate() {}

	/**
	 * Saves the offset and hands the message of the update, when it has one, to the bot. A repeated delivery does nothing.
	 * @param json the update, with {@code botName}
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String botName = json.getAsString(JnJsonInstantMessengerFields.botName);
		Long updateId = json.getAsLongNumber(Fields.update_id);
		long nextOffset = updateId + 1;
		CcpJsonRepresentation offsetKey = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.botName, botName);
		CcpEntityMetaData entityMetaData = JbEntityBotUpdateId.ENTITY.getEntityMetaData();
		CcpJsonRepresentation savedOffset = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(offsetKey, notFound -> notFound.put(JbEntityBotUpdateId.Fields.updateId, FIRST_OFFSET));
		Long lastOffset = savedOffset.getAsLongNumber(JbEntityBotUpdateId.Fields.updateId);
		boolean repeatedDelivery = lastOffset == nextOffset;

		if(repeatedDelivery) {
			return json;
		}

		CcpJsonRepresentation offsetToSave = offsetKey.put(JbEntityBotUpdateId.Fields.updateId, nextOffset);
		JbEntityBotUpdateId.ENTITY.save(offsetToSave);

		boolean thereIsNoMessage = false == json.containsAllFields(JnJsonInstantMessengerFields.message);

		if(thereIsNoMessage) {
			return json;
		}

		CcpJsonRepresentation message = this.getSimplifiedMessage(json, botName, updateId);
		JbBotType botType = JbBotType.valueOf(botName);
		CcpBusiness bot = botType.getBot();
		bot.execute(message);
		return json;
	}

	/**
	 * Simplifies an update: bot, chat id, message id, send time, update id, user name and text.
	 * @param json the update
	 * @param botName the bot
	 * @param updateId the id of the update
	 * @return the simplified message
	 */
	private CcpJsonRepresentation getSimplifiedMessage(CcpJsonRepresentation json, String botName, Long updateId) {
		CcpJsonRepresentation telegramMessage = json.getInnerJson(JnJsonInstantMessengerFields.message);
		CcpJsonRepresentation chat = telegramMessage.getInnerJson(Fields.chat);
		CcpJsonRepresentation sender = telegramMessage.getInnerJson(Fields.from);
		Long chatId = chat.getAsLongNumber(JnJsonCommonsFields.id);
		Long messageId = telegramMessage.getAsLongNumber(CcpJsonCommonsFields.message_id);
		Long sentAt = telegramMessage.getAsLongNumber(JnJsonCommonsFields.date);
		String userName = sender.getAsString(Fields.username);
		String typedValue = telegramMessage.getAsString(CcpJsonCommonsFields.text);

		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonInstantMessengerFields.botName, botName)
				.put(JnJsonInstantMessengerFields.chatId, chatId)
				.put(CcpJsonCommonsFields.message_id, messageId)
				.put(Fields.sentAt, sentAt)
				.put(Fields.updateId, updateId)
				.put(Fields.userName, userName)
				.put(JnJsonInstantMessengerFields.message, typedValue)
				;
		return message;
	}
}
