package com.jb.business.bots.ignored.users;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.query.CcpQueryExecutorDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillPending;

/**
 * The users ignored by the support ({@link VisEntityCommandNotAllowedToUser}) and the texts that show them, shared by
 * the steps of the {@code ignoredUsers} command.
 *
 * <p>The users are found by a query (a support bot command may run one: only the support team issues them), which
 * Elasticsearch answers from what was refreshed up to a second ago, so each user found is confirmed by its id, read
 * in real time: without that, a user the operator has just stopped ignoring would still be listed. The email of the
 * record is kept as hash, so the list takes the readable one from the {@code description} (written there since
 * 2026-10-11); a user ignored before that cannot be shown by email and is left out of the list.
 */
final class JbSupportIgnoredUsersList {

	/** Utility class; not instantiable. */
	private JbSupportIgnoredUsersList() {}

	/**
	 * The ignored users, ordered by email, each one with its readable {@code email}, the {@code commandName} in which
	 * the operator ignored them and the {@code description} of the request.
	 * @return the ignored users
	 */
	static List<CcpJsonRepresentation> getIgnoredUsers() {

		CcpQueryOptions requestWithEveryUser = CcpQueryOptions.INSTANCE.matchAll();
		CcpQueryOptions requestWithAllResults = requestWithEveryUser.maxResults();
		CcpQueryExecutorDecorator ignoredUsersQuery = requestWithAllResults.selectFrom(VisEntityCommandNotAllowedToUser.ENTITY);
		List<CcpJsonRepresentation> resultList = ignoredUsersQuery.getResultAsList();

		Stream<CcpJsonRepresentation> resultStream = resultList.stream();
		Stream<CcpJsonRepresentation> readableUsersStream = resultStream.map(JbSupportIgnoredUsersList::withReadableEmail);
		Stream<CcpJsonRepresentation> usersWithEmailStream = readableUsersStream.filter(user -> false == user.getAsString(JnJsonCommonsFields.email).isEmpty());
		List<CcpJsonRepresentation> foundUsers = usersWithEmailStream.collect(Collectors.toList());
		boolean nothingFound = foundUsers.isEmpty();

		if(nothingFound) {
			return foundUsers;
		}

		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpJsonRepresentation[] foundUsersArray = foundUsers.toArray(new CcpJsonRepresentation[foundUsers.size()]);
		CcpSelectUnionAll usersById = crud.unionAll(foundUsersArray, JnDeleteKeysFromCache.INSTANCE, VisEntityCommandNotAllowedToUser.ENTITY);
		Stream<CcpJsonRepresentation> usersToConfirmStream = foundUsers.stream();
		Stream<CcpJsonRepresentation> existingUsersStream = usersToConfirmStream.filter(user -> VisEntityCommandNotAllowedToUser.ENTITY.isPresentInThisUnionAll(usersById, user));
		Comparator<CcpJsonRepresentation> byEmail = Comparator.comparing(user -> user.getAsString(JnJsonCommonsFields.email));
		Stream<CcpJsonRepresentation> sortedUsersStream = existingUsersStream.sorted(byEmail);
		List<CcpJsonRepresentation> existingUsers = sortedUsersStream.collect(Collectors.toList());
		return existingUsers;
	}

	/**
	 * Puts in place of the hash of the email the readable one kept in the {@code description} (empty when the user was
	 * ignored before it started being kept there).
	 * @param ignoredUser the record found
	 * @return the record with the readable email
	 */
	private static CcpJsonRepresentation withReadableEmail(CcpJsonRepresentation ignoredUser) {
		CcpJsonRepresentation description = ignoredUser.getInnerJson(VisEntityCommandNotAllowedToUser.Fields.description);
		String readableEmail = description.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation userWithReadableEmail = ignoredUser.put(JnJsonCommonsFields.email, readableEmail);
		return userWithReadableEmail;
	}

	/**
	 * The emails of the ignored users, in the order of the list.
	 * @param ignoredUsers the ignored users
	 * @return the emails
	 */
	static List<String> getEmails(List<CcpJsonRepresentation> ignoredUsers) {
		Stream<CcpJsonRepresentation> ignoredUsersStream = ignoredUsers.stream();
		Stream<String> emailsStream = ignoredUsersStream.map(user -> user.getAsString(JnJsonCommonsFields.email));
		List<String> emails = emailsStream.collect(Collectors.toList());
		return emails;
	}

	/**
	 * Returns the language of the session (the enum itself or its name), Portuguese by default.
	 * @param json the session
	 * @return the language
	 */
	static JnLanguage getLanguage(CcpJsonRepresentation json) {
		// the language may be in the json as the enum itself or as its name
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		return language;
	}

	/**
	 * How many users are ignored, followed by the first one.
	 * @param language the language
	 * @param ignoredUsers the ignored users
	 * @return the text
	 */
	static String getListText(JnLanguage language, List<CcpJsonRepresentation> ignoredUsers) {
		int usersCount = ignoredUsers.size();
		boolean singleUser = usersCount == 1;
		JbSupportIgnoredUsersMessages header = singleUser ? JbSupportIgnoredUsersMessages.oneUser : JbSupportIgnoredUsersMessages.manyUsers;
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportIgnoredUsersMessageFields.usersCount, usersCount);
		String headerText = header.getMessage(language, parameters);
		String firstUserPrompt = getUserPrompt(language, ignoredUsers, 0);
		String listText = headerText + "\n" + firstUserPrompt;
		return listText;
	}

	/**
	 * Shows the user at the given position (email, the command in which they were ignored and what they had asked)
	 * and asks whether to stop ignoring them or go to the next one.
	 * @param language the language
	 * @param ignoredUsers the ignored users
	 * @param userIndex the position of the user
	 * @return the text
	 */
	static String getUserPrompt(JnLanguage language, List<CcpJsonRepresentation> ignoredUsers, int userIndex) {
		int userNumber = userIndex + 1;
		int usersCount = ignoredUsers.size();
		CcpJsonRepresentation ignoredUser = ignoredUsers.get(userIndex);
		String email = ignoredUser.getAsString(JnJsonCommonsFields.email);
		String commandName = ignoredUser.getAsString(VisEntityCommandNotAllowedToUser.Fields.commandName);
		CcpJsonRepresentation description = ignoredUser.getInnerJson(VisEntityCommandNotAllowedToUser.Fields.description);
		// the skill of a skill suggestion, or the parent of a skill hierarchy fix
		String suggestedSkill = description.getAsString(VisEntitySkillPending.Fields.skill);
		String parent = description.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
		String request = suggestedSkill.isEmpty() ? parent : suggestedSkill;

		CcpJsonRepresentation parametersWithNumber = CcpOtherConstants.EMPTY_JSON.put(JbSupportIgnoredUsersMessageFields.userNumber, userNumber);
		CcpJsonRepresentation parametersWithCount = parametersWithNumber.put(JbSupportIgnoredUsersMessageFields.usersCount, usersCount);
		CcpJsonRepresentation parametersWithEmail = parametersWithCount.put(JbSupportIgnoredUsersMessageFields.email, email);
		CcpJsonRepresentation parametersWithCommand = parametersWithEmail.put(JbSupportIgnoredUsersMessageFields.commandName, commandName);
		CcpJsonRepresentation parameters = parametersWithCommand.put(JbSupportIgnoredUsersMessageFields.request, request);

		String userPrompt = JbSupportIgnoredUsersMessages.userPrompt.getMessage(language, parameters);
		return userPrompt;
	}

	/**
	 * Returns a message about the user in the language.
	 * @param language the language
	 * @param message the message
	 * @param email the email of the user
	 * @return the message
	 */
	static String getMessageAboutTheUser(JnLanguage language, JbSupportIgnoredUsersMessages message, String email) {
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportIgnoredUsersMessageFields.email, email);
		String messageAboutTheUser = message.getMessage(language, parameters);
		return messageAboutTheUser;
	}

	/**
	 * Ends the session telling the operator that no user is ignored.
	 * @param json the session
	 * @param language the language
	 * @return never returns: throws the flow diversion
	 */
	static CcpJsonRepresentation noIgnoredUser(CcpJsonRepresentation json, JnLanguage language) {
		String noIgnoredUser = JbSupportIgnoredUsersMessages.noIgnoredUser.getMessage(language);
		CcpJsonRepresentation jsonWithReply = json.put(JbSupportIgnoredUsersFields.botReply, noIgnoredUser);
		CcpJsonRepresentation ended = JbSupportIgnoredUsersStatus.noIgnoredUser.throwException(jsonWithReply);
		return ended;
	}
}
