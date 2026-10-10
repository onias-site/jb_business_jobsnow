package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jb.entities.JbEntityBotChatLanguage;
import com.jn.entities.JnEntitySystemMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.jn.utils.JnSystemProperties;

/**
 * The language a chat talks to a bot in: the one the operator chose with {@code /setLanguage}
 * ({@link JbEntityBotChatLanguage}), or the one configured for the system ({@code supportLanguage}) while there is no
 * choice. Only the languages the bot texts were written in can be chosen.
 */
class BotChatLanguage {

	/** Not instantiable. */
	private BotChatLanguage() {
	}

	/**
	 * The key of the chat language record: the bot and the chat id as a whole number (read from a parsed JSON it is a
	 * double, and the id calculated over it would be another one).
	 * @param json the message or the session
	 * @return the key
	 */
	static CcpJsonRepresentation getKey(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		CcpJsonRepresentation botName = json.getJsonPiece(JnJsonInstantMessengerFields.botName);
		CcpJsonRepresentation key = botName.put(JnJsonInstantMessengerFields.chatId, chatId);
		return key;
	}

	/**
	 * The language of the chat.
	 * @param json the message or the session, with {@code botName} and {@code chatId}
	 * @return the name of the language
	 */
	static String getChatLanguage(CcpJsonRepresentation json) {
		CcpJsonRepresentation key = getKey(json);
		String systemLanguage = JnSystemProperties.INSTANCE.supportLanguage();
		CcpBusiness noChoiceYet = notFound -> key.put(JnJsonCommonsFields.language, systemLanguage);
		CcpEntityMetaData entityMetaData = JbEntityBotChatLanguage.ENTITY.getEntityMetaData();
		CcpJsonRepresentation chatLanguage = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(key, noChoiceYet);
		String language = chatLanguage.getAsString(JnJsonCommonsFields.language);
		return language;
	}

	/**
	 * Saves the language chosen for the chat.
	 * @param json the message or the session, with {@code botName} and {@code chatId}
	 * @param language the language chosen
	 */
	static void setChatLanguage(CcpJsonRepresentation json, JnLanguage language) {
		CcpJsonRepresentation key = getKey(json);
		String languageName = language.name();
		CcpJsonRepresentation chatLanguage = key.put(JnJsonCommonsFields.language, languageName);
		JbEntityBotChatLanguage.ENTITY.save(chatLanguage);
	}

	/**
	 * The languages that can be chosen: the ones the bot texts were written in (the ones with their own record of
	 * {@link JbDefaultBotCommandMessages#languageSet}, without the fallback to English).
	 * @return the languages
	 */
	static List<JnLanguage> getAvailableLanguages() {
		JnLanguage[] languages = JnLanguage.values();
		List<JnLanguage> availableLanguages = new ArrayList<>();
		for (JnLanguage language : languages) {
			CcpJsonRepresentation systemMessageId = JbDefaultBotCommandMessages.languageSet.getSystemMessageId(language);
			boolean notWritten = false == JnEntitySystemMessage.ENTITY.exists(systemMessageId);
			if(notWritten) {
				continue;
			}
			availableLanguages.add(language);
		}
		return availableLanguages;
	}

	/**
	 * The names of the available languages in the given language, separated by commas.
	 * @param language the language of the names
	 * @return the names
	 */
	static String getAvailableLanguageNames(JnLanguage language) {
		List<JnLanguage> availableLanguages = getAvailableLanguages();
		List<String> names = new ArrayList<>();
		for (JnLanguage availableLanguage : availableLanguages) {
			String name = getLanguageName(availableLanguage, language);
			names.add(name);
		}
		String availableLanguageNames = String.join(", ", names);
		return availableLanguageNames;
	}

	/**
	 * The name of a language written in another one.
	 * @param language the language named
	 * @param languageOfTheName the language the name is written in
	 * @return the name
	 */
	static String getLanguageName(JnLanguage language, JnLanguage languageOfTheName) {
		String languageItemName = language.name();
		JbLanguageNames languageNames = JbLanguageNames.valueOf(languageItemName);
		String name = languageNames.getMessage(languageOfTheName);
		return name;
	}

	/**
	 * Tells whether the typed text names the language, by its canonical name or by its name in any available
	 * language, ignoring case and accents ({@code portugues}, {@code Português}, {@code portuguese}).
	 * @param typedLanguage the typed text
	 * @param language the language
	 * @return {@code true} when it names the language
	 */
	static boolean names(String typedLanguage, JnLanguage language) {
		String typed = comparable(typedLanguage);
		String canonicalName = language.name();
		String canonical = comparable(canonicalName);
		boolean typedTheCanonicalName = typed.equals(canonical);

		if(typedTheCanonicalName) {
			return true;
		}

		List<JnLanguage> availableLanguages = getAvailableLanguages();
		for (JnLanguage languageOfTheName : availableLanguages) {
			String name = getLanguageName(language, languageOfTheName);
			String comparableName = comparable(name);
			boolean typedThisName = typed.equals(comparableName);
			if(typedThisName) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The text without accents, in lower case and without surrounding spaces.
	 * @param text the text
	 * @return the comparable text
	 */
	private static String comparable(String text) {
		CcpStringDecorator stringDecorator = new CcpStringDecorator(text);
		CcpTextDecorator textDecorator = stringDecorator.text();
		CcpTextDecorator withoutAccents = textDecorator.stripAccents();
		String content = withoutAccents.content;
		String trimmed = content.trim();
		String lowerCase = trimmed.toLowerCase();
		return lowerCase;
	}
}
