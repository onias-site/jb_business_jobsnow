package com.jb.business.bots.ignored.users;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;

/**
 * First step of the {@code ignoredUsers} command ({@code /ignoredUsers}): tells the operator how many users the
 * support is ignoring and shows the first one, asking whether to stop ignoring them or go to the next one, the same
 * interaction of {@code /pendingTickets}. Without any ignored user the session ends with that notice
 * ({@code noIgnoredUser}).
 */
public class JbSupportIgnoredUsersShow implements CcpBusiness {

	/**
	 * Shows the count and the first ignored user.
	 * @param json the session
	 * @return the session with the reply and the user shown
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		List<CcpJsonRepresentation> ignoredUsers = JbSupportIgnoredUsersList.getIgnoredUsers();
		JnLanguage language = JbSupportIgnoredUsersList.getLanguage(json);
		boolean noIgnoredUser = ignoredUsers.isEmpty();

		if(noIgnoredUser) {
			CcpJsonRepresentation ended = JbSupportIgnoredUsersList.noIgnoredUser(json, language);
			return ended;
		}

		String listText = JbSupportIgnoredUsersList.getListText(language, ignoredUsers);
		List<String> emails = JbSupportIgnoredUsersList.getEmails(ignoredUsers);
		String firstEmail = emails.get(0);
		CcpJsonRepresentation jsonWithReply = json.put(JbSupportIgnoredUsersFields.botReply, listText);
		CcpJsonRepresentation jsonWithTheUser = jsonWithReply.put(JbSupportIgnoredUsersFields.currentUser, firstEmail);
		return jsonWithTheUser;
	}
}
