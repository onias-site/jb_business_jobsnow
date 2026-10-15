package com.jb.business.bots.engine;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.query.CcpQueryExecutorDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.jn.entities.JnEntityBotCommand;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/** The bots of the platform. */
public enum JbBotType implements CcpJsonFieldName{
	/** The bot that talks to the users; open to everybody. */
	user {
		/**
		 * Open to everybody.
		 * @return {@code false}
		 */
		@Override
		protected boolean isRestricted() {
			return false;
		}
	},
	/** The support bot; restricted to the allowed users. */
	support {
		/**
		 * Restricted to the allowed users.
		 * @return {@code true}
		 */
		protected boolean isRestricted() {
			return true;
		}
	};
	
	/**
	 * Returns the key to search the records of the bot.
	 * @return a supplier of {@code botName}
	 */
	Supplier<CcpJsonRepresentation> getParameterToSearchBot() {
		String botName =  this.name();
		var parameterToSearchBots = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonInstantMessengerFields.botName, botName)
		;
		Supplier<CcpJsonRepresentation> jsonSupplier = parameterToSearchBots.getJsonSupplier();
		return jsonSupplier;
	}
	
	/**
	 * Returns the names of the commands configured for the bot: the records of {@code JnEntityBotCommand} with its
	 * {@code botName}, seeded by every cost center that adds commands to the bot (jb and vis, for the support bot). Read
	 * when the bot starts, which is not an online request, so querying by {@code botName} is allowed (OQL). Until
	 * 2026-10-15 the commands were a list in {@code jb_bot}, seeded only by jb, which then had to know every command.
	 * @return the canonical names of the commands, without the default ones, in alphabetical order (the order of the
	 * search is not stable, and {@code /showAllCommands} lists them in this order)
	 */
	List<String> getConfiguredCommands() {
		String botName = this.name();
		CcpQueryOptions commandsOfTheBot = CcpQueryOptions.INSTANCE
				.startQuery()
				.startBool()
				.startMust()
				.term(JnJsonInstantMessengerFields.botName, botName)
				.endMustAndBackToBool()
				.endBoolAndBackToQuery()
				.endQueryAndBackToRequest()
				.maxResults();
		CcpQueryExecutorDecorator commandsQuery = commandsOfTheBot.selectFrom(JnEntityBotCommand.ENTITY);
		List<CcpJsonRepresentation> commands = commandsQuery.getResultAsList();
		Stream<CcpJsonRepresentation> commandsStream = commands.stream();
		Stream<String> commandNames = commandsStream.map(command -> command.getAsString(JnJsonInstantMessengerFields.commandName));
		Stream<String> sortedCommandNames = commandNames.sorted();
		List<String> configuredCommands = sortedCommandNames.collect(Collectors.toList());
		return configuredCommands;
	}

	/**
	 * Returns the bot loaded by the engine.
	 * @return the bot
	 */
	public Bot getBot() {
		Bot bot = JbBotEngine.INSTANCE.allBots.get(this);
		return bot;
	}
	
	/**
	 * Tells whether only the allowed users may use the bot.
	 * @return {@code true} for a restricted bot
	 */
	abstract protected boolean isRestricted();
}
