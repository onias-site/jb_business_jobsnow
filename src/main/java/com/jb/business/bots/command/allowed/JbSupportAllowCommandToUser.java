package com.jb.business.bots.command.allowed;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityCommandNotAllowedToUser;

/**
 * Single step of the {@code allowCommandToUser} command ({@code /allowCommandToUser <command> <email>}): undoes
 * the decision of ignoring the user for the command, removing the user from
 * {@link VisEntityCommandNotAllowedToUser}, so that the next requests of the user reach the operator again. The
 * delete moves the record to the twin, vis_command_reallowed_to_user, which keeps it for control and tracking.
 * Without a record for the email and the command the flow is diverted with {@code userNotIgnored}.
 */
public class JbSupportAllowCommandToUser implements CcpBusiness {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String email = json.getAsString(JnJsonCommonsFields.email);
		String command = json.getAsString(JbSupportAllowCommandToUserFields.command);
		CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
		CcpJsonRepresentation ignoredUser = ignoredUserWithEmail.put(VisEntityCommandNotAllowedToUser.Fields.commandName, command);

		boolean deleted = VisEntityCommandNotAllowedToUser.ENTITY.delete(ignoredUser);

		if(false == deleted) {
			CcpJsonRepresentation userNotIgnored = JbSupportAllowCommandToUserStatus.userNotIgnored.throwException(json);
			return userNotIgnored;
		}

		return json;
	}
}
