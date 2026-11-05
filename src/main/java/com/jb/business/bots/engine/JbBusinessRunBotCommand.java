package com.jb.business.bots.engine;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * Runs a message of a bot in the queue of its command: {@link JbBusinessReceiveBotUpdate} publishes each message to
 * {@code JbBusinessRunBotCommand~<command>}, one topic and one subscription per command, each with a single consumer
 * thread (since 2026-11-04). The bot loads the session and runs the step as it always did; a step that redirects to
 * another command (choosing a ticket in {@code /pendingTickets}) runs that command in this same thread.
 */
public class JbBusinessRunBotCommand implements CcpBusiness {

	/** The single instance. */
	public static final JbBusinessRunBotCommand INSTANCE = new JbBusinessRunBotCommand();

	private JbBusinessRunBotCommand() {}

	/**
	 * Hands the message to its bot.
	 * @param json the simplified message, with {@code botName}
	 * @return what the bot answers
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String botName = json.getAsString(JnJsonInstantMessengerFields.botName);
		JbBotType botType = JbBotType.valueOf(botName);
		Bot bot = botType.getBot();
		CcpJsonRepresentation answer = bot.execute(json);
		return answer;
	}
}
