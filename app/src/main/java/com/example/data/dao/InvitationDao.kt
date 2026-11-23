package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvitationDao {

    @Query("SELECT * FROM invitations WHERE householdId = :householdId ORDER BY createdAt DESC")
    fun getInvitationsByHousehold(householdId: String): Flow<List<InvitationEntity>>

    @Query("SELECT * FROM invitations WHERE householdId = :householdId ORDER BY createdAt DESC")
    suspend fun getInvitationsByHouseholdOnce(householdId: String): List<InvitationEntity>

    @Query("SELECT * FROM invitations WHERE invitationTokenHash = :tokenHash LIMIT 1")
    suspend fun getInvitationByTokenHash(tokenHash: String): InvitationEntity?

    @Query("SELECT * FROM invitations WHERE id = :id LIMIT 1")
    suspend fun getInvitationById(id: String): InvitationEntity?

    @Query(
        """
        SELECT * FROM invitations
        WHERE householdId = :householdId
          AND LOWER(inviteeEmail) = LOWER(:email)
          AND status = 'PENDING'
          AND expiresAt > :nowMillis
        LIMIT 1
        """
    )
    suspend fun findActivePendingByEmail(
        householdId: String,
        email: String,
        nowMillis: Long = System.currentTimeMillis()
    ): InvitationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: InvitationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitations(invitations: List<InvitationEntity>)

    @Update
    suspend fun updateInvitation(invitation: InvitationEntity)

    @Query("DELETE FROM invitations WHERE householdId = :householdId")
    suspend fun deleteInvitationsForHousehold(householdId: String)
}
