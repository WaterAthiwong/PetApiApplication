package com.champaca.inventorydata.masterdata.user

import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.UserDao
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class UserRepository {

    @Cacheable("usernames")
    fun findByUsername(username: String): UserDao? {
        return UserDao.find { (User.username eq username) and (User.status eq "A") }.singleOrNull()
    }

    @Cacheable("userIds")
    fun findById(id: Int?): UserDao? {
        if (id == null) {
            return null
        }
        return UserDao.findById(id)
    }

    fun getAll(): List<UserDao> {
        return UserDao.find { User.status eq "A" }.toList()
    }

    @Cacheable("userHasProcessTypes")
    fun checkUserHasProcessApprovalPermission(userId: Int, processTypeId: Int): Boolean {
        return UserHasProcessType
            .select(UserHasProcessType.userId)
            .where { (UserHasProcessType.userId eq userId) and (UserHasProcessType.processTypeId eq processTypeId) }
            .count() > 0
    }

    @Cacheable("userHasPermissions")
    fun getUserPermissions(userId: Int): List<String> {
        val joins = User.join(UserHasGroup, JoinType.INNER) { User.id eq UserHasGroup.userId }
            .join(Group, JoinType.INNER) { Group.id eq UserHasGroup.groupId }
            .join(GroupHasPermission, JoinType.INNER) { Group.id eq GroupHasPermission.groupId }
            .join(Permission, JoinType.INNER) { Permission.id eq GroupHasPermission.permissionId }
        val query = joins.select(Permission.name)
            .where { (User.id eq userId) }
            .distinct()
        return query.map { it[Permission.name] }
    }
}