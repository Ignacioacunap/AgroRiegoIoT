package cl.inacap.agroriego

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvEstadoTierra: TextView
    private lateinit var switchModo: SwitchCompat
    private lateinit var btnAbrir: Button
    private lateinit var btnCerrar: Button
    private lateinit var btnConectar: Button
    private lateinit var tvEstadoConexion: TextView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvEstadoTierra = findViewById(R.id.tvEstadoTierra)
        switchModo = findViewById(R.id.switchModo)
        btnAbrir = findViewById(R.id.btnAbrir)
        btnCerrar = findViewById(R.id.btnCerrar)
        btnConectar = findViewById(R.id.btnConectar)
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion)

        btnLogin.setOnClickListener {
            val user = etUsuario.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            if (user == "admin" && pass == "1234") {
                Toast.makeText(this, "Autenticación exitosa", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Credenciales incorrectas (Usa admin / 1234)", Toast.LENGTH_SHORT).show()
            }
        }

        verificarPermisosBluetooth()

        btnConectar.setOnClickListener {
            conectarBluetooth()
        }

        btnAbrir.setOnClickListener {
            enviarComandoSerial('A')
            tvEstadoTierra.text = "Estado: Válvula ABIERTA (Manual)"
        }

        btnCerrar.setOnClickListener {
            enviarComandoSerial('C')
            tvEstadoTierra.text = "Estado: Válvula CERRADA (Manual)"
        }

        switchModo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enviarComandoSerial('M')
                tvEstadoTierra.text = "Estado: Modo Automático Activado"
            } else {
                tvEstadoTierra.text = "Estado: Modo Manual"
            }
        }
    }

    private fun verificarPermisosBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                    101
                )
            }
        }
    }

    private fun conectarBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Enciende el Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 101)
                return
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        var targetDevice: BluetoothDevice? = null
        pairedDevices?.forEach { device ->
            if (device.name.contains("HC-05") || device.name.contains("HC-06") || device.name.contains("Arduino")) {
                targetDevice = device
            }
        }

        if (targetDevice != null) {
            try {
                bluetoothSocket = targetDevice?.createRfcommSocketToServiceRecord(appUuid)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                tvEstadoConexion.text = "Estado: Conectado a ${targetDevice?.name}"
                Toast.makeText(this, "Conectado al HC-05", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                tvEstadoConexion.text = "Estado: Error al conectar"
            }
        } else {
            Toast.makeText(this, "No se encontró el HC-05 emparejado", Toast.LENGTH_LONG).show()
        }
    }

    private fun enviarComandoSerial(comando: Char) {
        if (outputStream != null) {
            try {
                outputStream?.write(comando.code)
                Toast.makeText(this, "Comando enviado: $comando", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Conéctate al Bluetooth primero", Toast.LENGTH_SHORT).show()
        }
    }
}