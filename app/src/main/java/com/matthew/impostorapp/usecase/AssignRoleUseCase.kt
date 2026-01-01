package com.matthew.impostorapp.usecase

import com.matthew.impostorapp.domain.model.Player
import com.matthew.impostorapp.domain.model.Role

class AssignRoleUseCase {

    fun execute(playerCount: Int, impostorCount: Int): List<Player> {
        require(impostorCount < playerCount)

        val roles = MutableList(playerCount) { Role.PLAYER }
        repeat(impostorCount) { roles[it] = Role.IMPOSTOR }
        roles.shuffle()

        return roles.map { Player(role = it) }
    }
}
