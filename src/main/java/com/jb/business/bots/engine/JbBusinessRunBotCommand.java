package com.jb.business.bots.engine;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * Runs a message of a bot in the queue of its command: {@link JbBusinessReceiveBotUpdate} publishes each message to
 * {@code JbBusinessRunBotCommand~<bot>~<command>}, one topic and one subscription per command of each bot (since
 * 2026-11-04; the bot entered the name on 2026-11-05): 1 consumer thread for each command of the support bot, 4 for each
 * one of the user bot. Its consumers keep the synchronous sender (no column {@code senderClass} in {@code topicos.tsv},
 * decided on 2026-11-05 to spare a trip to the network): what a step publishes runs in this same thread. The bot loads the session and runs the step as it always did; a step that redirects to
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
