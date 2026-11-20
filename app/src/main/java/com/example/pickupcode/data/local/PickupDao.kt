package com.example.pickupcode.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PickupDao {

    @Query(
        """
        SELECT * FROM pickup_items
        ORDER BY created_at DESC, id DESC
        """
    )
    fun observeAllItems(): Flow<List<PickupItemEntity>>

    @Query(
        """
        SELECT * FROM pickup_items
        WHERE UPPER(code) = UPPER(:code)
          AND created_at >= :sinceTimestamp
        ORDER BY created_at DESC
        """
    )
    suspend fun findRecentByCode(code: String, sinceTimestamp: Long): List<PickupItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PickupItemEntity): Long

    @Update
    suspend fun update(item: PickupItemEntity)

    @Query(
        """
        UPDATE pickup_items
        SET is_picked_up = :isPickedUp,
            picked_up_at = :pickedUpAt,
            updated_at = :updatedAt
        WHERE id IN (:ids)
        """
    )
    suspend fun updatePickedUpStatusBatch(
        ids: List<Long>,
        isPickedUp: Boolean,
        pickedUpAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE pickup_items
        SET product_name = :productName,
            is_custom_remark = :isCustomRemark,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateProductName(
        id: Long,
        productName: String,
        isCustomRemark: Boolean = true,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE pickup_items
        SET station_number = :stationNumber,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateStationNumber(
        id: Long,
        stationNumber: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM pickup_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pickup_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * 清理旧版本遗留的演示示例数据
     */
    @Query(
        """
        DELETE FROM pickup_items
        WHERE code IN ('3-2-1045', '3-2-1046', '5-1-2088', '1-4-3092', '2-1-8831', 'SF6-2019', '83920416', '7-3-5512')
          AND sender_or_package IN ('106903290188', 'com.taobao.taobao', '106980009531', '95338', '1069012355', '1069200911')
        """
    )
    suspend fun deleteLegacyDemoItems(): Int
}
