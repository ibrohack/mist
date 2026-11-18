package com.mist.service;

import com.mist.dao.UserDao;
import com.mist.exception.MistException;
import com.mist.exception.NotFoundException;
import com.mist.exception.UnauthorizedException;
import com.mist.model.Developer;
import com.mist.model.Player;
import com.mist.model.User;
import org.bson.types.ObjectId;

/**
 * Loads the account that performs an action and checks that it may act: it must exist, be active, and have the right
 * role. Shared by the services, which receive the acting user's id from the UI.
 */
final class Accounts {

    /** Not instantiable: a holder of account lookups. */
    private Accounts() {
    }

    /**
     * Loads an active account of any role.
     *
     * @param users the user DAO
     * @param id    the account id
     * @return the account
     * @throws NotFoundException     if there is no such account
     * @throws UnauthorizedException if it was deactivated
     * @throws MistException         if the database fails
     */
    static User active(UserDao users, ObjectId id) throws MistException {
        if (id == null) {
            throw new NotFoundException(ServiceKeys.USER_NOT_FOUND);
        }
        User user = users.findById(id).orElseThrow(() -> new NotFoundException(ServiceKeys.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new UnauthorizedException(ServiceKeys.ACCOUNT_DEACTIVATED);
        }
        return user;
    }

    /**
     * Loads an active player.
     *
     * @param users the user DAO
     * @param id    the player id
     * @return the player
     * @throws NotFoundException     if there is no such account
     * @throws UnauthorizedException if it was deactivated or is not a player
     * @throws MistException         if the database fails
     */
    static Player activePlayer(UserDao users, ObjectId id) throws MistException {
        if (active(users, id) instanceof Player player) {
            return player;
        }
        throw new UnauthorizedException(ServiceKeys.USER_NOT_PLAYER);
    }

    /**
     * Loads an active developer.
     *
     * @param users the user DAO
     * @param id    the developer id
     * @return the developer
     * @throws NotFoundException     if there is no such account
     * @throws UnauthorizedException if it was deactivated or is not a developer
     * @throws MistException         if the database fails
     */
    static Developer activeDeveloper(UserDao users, ObjectId id) throws MistException {
        if (active(users, id) instanceof Developer developer) {
            return developer;
        }
        throw new UnauthorizedException(ServiceKeys.USER_NOT_DEVELOPER);
    }

    /**
     * Loads a developer of any status, for example the developer of a game being sold or returned.
     *
     * @param users the user DAO
     * @param id    the developer id
     * @return the developer
     * @throws NotFoundException if there is no developer with that id
     * @throws MistException     if the database fails
     */
    static Developer developer(UserDao users, ObjectId id) throws MistException {
        if (users.findById(id).orElse(null) instanceof Developer developer) {
            return developer;
        }
        throw new NotFoundException(ServiceKeys.USER_NOT_FOUND);
    }
}
