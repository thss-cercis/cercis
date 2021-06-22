package cn.cercis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cn.cercis.common.LOG_TAG
import cn.cercis.util.helper.openApplicationSettingsPage
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

// todo: 谁来都好没时间了..
@AndroidEntryPoint
class SelectLocationActivity : AppCompatActivity() {
    companion object {
        const val PERMISSION_REQUEST_CODE = 100
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    private var isNeedCheck = true

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    }

    override fun onResume() {
        super.onResume()
        if (isNeedCheck) {
            checkPermissions(requiredPermissions)
        }
    }

    private fun checkPermissions(permissions: Array<String>) {
        try {
            val needRequestPermissionList = findDeniedPermissions(permissions)
            if (null != needRequestPermissionList && needRequestPermissionList.isNotEmpty()
            ) {
                val array = needRequestPermissionList.toTypedArray()
                requestPermissions(array, PERMISSION_REQUEST_CODE)
            } else {
                isNeedCheck = false
                initLocating()
            }
        } catch (e: Throwable) {
        }
    }

    /**
     * 获取权限集中需要申请权限的列表
     */
    private fun findDeniedPermissions(permissions: Array<String>): List<String>? {
        val needRequestPermissionList: MutableList<String> = ArrayList()
        try {
            for (perm in permissions) {
                if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED
                    || shouldShowRequestPermissionRationale(perm)
                ) {
                    needRequestPermissionList.add(perm)
                }
            }
        } catch (e: Throwable) {
        }
        return needRequestPermissionList
    }

    /**
     * 检测是否所有的权限都已经授权
     * @param grantResults
     * @return
     * @since 2.5.0
     */
    private fun verifyPermissions(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!verifyPermissions(grantResults)) {
                showMissingPermissionDialog()
            } else {
                initLocating()
            }
            isNeedCheck = false
        }
    }

    private fun showMissingPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.activity_select_location_permission_dialog_title)
            .setMessage(R.string.activity_select_location_permission_dialog_message)
            .setNegativeButton(R.string.activity_select_location_permission_dialog_cancel) { _, _ -> finish() }
            .setPositiveButton(R.string.activity_select_location_permission_dialog_settings) { _, _ -> openApplicationSettingsPage() }
            .setCancelable(false)
            .show()
    }

    private fun initLocating() {
        Log.d(LOG_TAG, "started locating")
        //初始化定位参数
        val locationClient = AMapLocationClient(this)
        val locationOption = AMapLocationClientOption()
        //设置返回地址信息，默认为true
        locationOption.isNeedAddress = true
        //获取一次定位结果，默认为false。
        locationOption.isOnceLocation = true;
        //设置定位监听
        locationClient.setLocationListener { amapLocation ->
            if (amapLocation != null) {
                if (amapLocation.errorCode == 0) {
                    amapLocation.locationType//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    amapLocation.latitude//获取纬度
                    amapLocation.longitude//获取经度
                    amapLocation.address//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                    amapLocation.aoiName//获取当前定位点的AOI信息
                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    Log.e(LOG_TAG,
                        "AmapError: location Error, ErrCode: ${amapLocation.errorCode} " +
                                "ErrInfo: ${amapLocation.errorInfo}")
                }
            }
        }
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        locationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        //设置定位间隔,单位毫秒,默认为2000ms
        locationOption.interval = 2000
        //设置定位参数
        locationClient.setLocationOption(locationOption)
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        locationClient.startLocation()
    }
}