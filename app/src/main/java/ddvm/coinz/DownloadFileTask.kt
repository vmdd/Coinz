package ddvm.coinz

import android.os.AsyncTask
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadFileTask(private val caller : DownloadCompleteListener) : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg urls: String): String =try {
        loadFileFromNetwork(urls[0])
    } catch(e: IOException) {
        "Unable to load content. Check your connection"
    }

    private fun loadFileFromNetwork(urlString: String) : String{
        val stream : InputStream = downloadUrl(urlString)
        //read from stream, build as string and return
        return stream.bufferedReader().use {it.readText()}
    }

    //Given a string representation of a URL, sets up a connection and gets an input stream
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String) : InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000 //miliseconds
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect()
        return conn.inputStream
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)

        caller.downloadComplete(result)
    }
}