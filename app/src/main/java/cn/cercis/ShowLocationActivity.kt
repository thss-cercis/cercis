package cn.cercis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.cercis.databinding.ActivityShowLocationBinding
import cn.cercis.util.helper.openApplicationSettingsPage
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint


/**
 * 使用 startActivity 启用此 activity 即可。
 * Intent 的 extra 中，需要设置 "location" 属性，值为 {@link cn.cercis.SelectedLocation} 类型.
 */
@AndroidEntryPoint
class ShowLocationActivity : AppCompatActivity() {
    companion object {
        const val PERMISSION_REQUEST_CODE = 100
        val requiredPermissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        const val RESULT_CODE_SUCCESS = 0
        const val RESULT_CODE_FAILURE = 0
    }

    private var isNeedCheck = true

    private lateinit var mapView: MapView
    private lateinit var aMap: AMap
    private var locDes: String? = null
    private var marker: Marker? = null
    private var targetLoc: SelectedLocation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityShowLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView = binding.selectMap
        mapView.onCreate(savedInstanceState)

        intent?.let {
            targetLoc = it.getParcelableExtra("location")
        }

        aMap = mapView.map
        // 自身蓝点样式
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER) ;//定位一次，且将视角移动到地图中心点。
        aMap.myLocationStyle = myLocationStyle // 设置定位蓝点的Style
        aMap.uiSettings.isMyLocationButtonEnabled = true // 设置默认定位按钮是否显示，非必需设置。
        aMap.isMyLocationEnabled = true // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.showIndoorMap(true)
        aMap.showBuildings(true)
        // 设置地图缩放等级
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17F))
        //
        val geocodeSearch = GeocodeSearch(this)
        geocodeSearch.setOnGeocodeSearchListener(object : OnGeocodeSearchListener {
            override fun onRegeocodeSearched(regeocodeResult: RegeocodeResult, i: Int) {
                locDes = regeocodeResult.regeocodeAddress.formatAddress
                marker?.let { it.snippet = locDes }
                // 设置标题
                binding.topAppBar.title = locDes
            }
            override fun onGeocodeSearched(geocodeResult: GeocodeResult, i: Int) {}
        })
        // 设置 marker
        targetLoc?.let {
            val markerOption = MarkerOptions()
            val lat = LatLng(it.latitude, it.longitude)
            markerOption.position(lat)
            markerOption.title("位置：")
            markerOption.draggable(false) //设置Marker可拖动
            // 将Marker设置为贴地显示，可以双指下拉地图查看效果
            val m = aMap.addMarker(markerOption)
            m.showInfoWindow()
            marker = m
            // 询问位置
            val query = RegeocodeQuery(LatLonPoint(lat.latitude, lat.longitude), 50F, GeocodeSearch.AMAP)
            geocodeSearch.getFromLocationAsyn(query)
            aMap.animateCamera(CameraUpdateFactory.newLatLng(lat))
        }

        binding.topAppBar.setNavigationOnClickListener {
            setResult(RESULT_CODE_FAILURE, Intent())
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(RESULT_CODE_FAILURE, Intent())
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (isNeedCheck) {
            checkPermissions(requiredPermissions)
        }
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState)
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
//                initLocating()
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
//                initLocating()
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
}
