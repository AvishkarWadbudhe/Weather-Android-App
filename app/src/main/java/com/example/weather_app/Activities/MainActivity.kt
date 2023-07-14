package com.example.weather_app.Activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.weather_app.Models.WeatherModel
import com.example.weather_app.R
import com.example.weather_app.Utilities.ApiUtilities
import com.example.weather_app.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Response
import java.math.RoundingMode


class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding
    private lateinit var currentLocation:Location
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE=101
    private val apiKey="74372341f206e3561cebab6b43ef8ead"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_main)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        binding.locationSearch.setOnEditorActionListener{textView,i,keyEvent ->
            if(i==EditorInfo.IME_ACTION_SEARCH)
            {
                getCityWeather(binding.locationSearch.text.toString())
                binding.locationSearch.text.clear()
                val view =this.currentFocus
                if(view!=null){
                    val imm:InputMethodManager=getSystemService(INPUT_METHOD_SERVICE)
                    as InputMethodManager

                    imm.hideSoftInputFromWindow(view.windowToken,0)
                    binding.locationSearch.clearFocus()
                }
                return@setOnEditorActionListener true
            }
            else{
                return@setOnEditorActionListener false
            }
        }

        binding.currentLocation.setOnClickListener{
            getCurrentLocation()
        }
        binding.searchOption.setOnClickListener{
            binding.location.visibility= View.GONE
            binding.location.visibility=View.VISIBLE
        }

        binding.back.setOnClickListener{
            hideKeyboard(this)

            binding.location.visibility=View.VISIBLE
            binding.location.visibility= View.GONE
        }


    }
    private fun hideKeyboard(activity:Activity)
    {
        val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        var view  = activity.currentFocus
        if(view==null)
        {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken,0)

    }
    private fun getCurrentLocation()
    {
        if(checkPermission())
        {
            if(isLocationEnabled())
            {
                if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                {
                    requestPermission()
                    return
                }
                fusedLocationProvider.lastLocation.addOnSuccessListener {
                    location->
                    if(location!=null)
                    {
                        currentLocation =location
                        binding.progressBar.visibility=View.VISIBLE

                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )

                    }

                }
            }
            else{
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            requestPermission()
        }


    }
    private fun getCityWeather(city:String)
    {
        binding.progressBar.visibility=View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(city,apiKey)
            ?.enqueue(object : retrofit2.Callback<WeatherModel> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                 if(response.isSuccessful)
                 {
                     binding.location.visibility=View.VISIBLE
                     binding.locationSearchBar.visibility=View.GONE
                     binding.progressBar.visibility=View.GONE

                     response.body()?.let {
                         setData(it)
                     }

                 }
                    else{
                     binding.location.visibility=View.VISIBLE
                     binding.locationSearchBar.visibility=View.GONE
                     binding.progressBar.visibility=View.GONE

                     Toast.makeText(this@MainActivity,"City Not Found",Toast.LENGTH_SHORT).show()
                 }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {

                }

            })
    }
    private fun fetchCurrentLocationWeather(latitude:String,longitude:String)
    {
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude,longitude,apiKey)
            ?.enqueue(object  :retrofit2.Callback<WeatherModel>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                   if(response.isSuccessful)
                   {
                       binding.progressBar.visibility=View.GONE

                       response.body()?.let {
                           setData(it)
                       }
                   }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {

                }

            })
    }
    private fun requestPermission()
    {
        ActivityCompat.requestPermissions(this,
        arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)


    }
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

private fun checkPermission():Boolean{
    if (ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)
        ==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){

        return true

    }
    return false
}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode==LOCATION_REQUEST_CODE)
        {
            if(grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                getCurrentLocation()
            }
            else{

            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body:WeatherModel)
    {
        binding.apply {
            selectedLocation.text=body.name
            weatherTemprature.text=""+k2c(body?.main?.temp!!)+"°"
            weatherState.text=body.weather[0].main
            weatherHumidity.text=body.main.humidity.toString()+"%"
            weatherWindSpeed.text=body.wind.speed.toString()+"m/s"

        }
        updateUI(body.weather[0].id)
    }
    private fun k2c(t:Double):Double{
        var intTemp =t
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1,RoundingMode.UP).toDouble()
    }
    private fun updateUI(id:Int)
    {
        binding.apply {
            when(id){

                //thunderStrom
                in 200..232->{
                    weatherLogo.setImageResource(R.drawable.ic_storm_weather)
                }
                //Drizzle
                in 300..321->{
                    weatherLogo.setImageResource(R.drawable.ic_few_clouds)
                }
                //rainy
                in 500..531->{
                    weatherLogo.setImageResource(R.drawable.ic_rainy_weather)
                }
                //snow
                in 600..622->{
                    weatherLogo.setImageResource(R.drawable.ic_snow_weather)
                }
                //broken clouds
                in 701..781->{
                    weatherLogo.setImageResource(R.drawable.ic_broken_clouds)
                }
                //clear
             800->{
                    weatherLogo.setImageResource(R.drawable.ic_clear_day)
                }
                //clouds
                in 801..804->{
                    weatherLogo.setImageResource(R.drawable.ic_cloudy_weather)
                }
                //
                else->{
                    weatherLogo.setImageResource(R.drawable.ic_unknown)
                }
            }
        }
    }

}



