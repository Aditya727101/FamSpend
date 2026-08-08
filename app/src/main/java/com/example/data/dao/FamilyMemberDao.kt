package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    @Query("SELECT * FROM family_members WHERE householdId = :householdId ORDER BY name ASC")
    fun getMembersByHousehold(householdId: String): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<FamilyMemberEntity>)

    @Update
    suspend fun updateMember(member: FamilyMemberEntity)

    @Query("UPDATE family_members SET isCurrentActiveUser = (id = :activeMemberId) WHERE householdId = :householdId")
    suspend fun setActiveMember(householdId: String, activeMemberId: String)

    @Delete
    suspend fun deleteMember(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE householdId = :householdId")
    suspend fun deleteAllMembersForHousehold(householdId: String)

    @Query("DELETE FROM family_members")
    suspend fun deleteAllMembers()
}
