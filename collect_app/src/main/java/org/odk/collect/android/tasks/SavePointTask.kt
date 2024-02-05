package org.odk.collect.android.tasks

import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.listeners.SavePointListener
import org.odk.collect.async.Scheduler
import org.odk.collect.async.SchedulerAsyncTaskMimic
import timber.log.Timber

class SavePointTask(
    private var listener: SavePointListener?,
    private val formController: FormController,
    val scheduler: Scheduler
) : SchedulerAsyncTaskMimic<Unit, Unit, String?>(scheduler) {
    private var priority: Int = ++lastPriorityUsed

    override fun onPreExecute() = Unit

    override fun doInBackground(vararg params: Unit): String? {
        if (priority < lastPriorityUsed) {
            Timber.w("Savepoint thread (p=%d) was cancelled (a) because another one is waiting (p=%d)", priority, lastPriorityUsed)
            return null
        }

        val start = System.currentTimeMillis()

        return try {
            val temp = SaveFormToDisk.getSavepointFile(formController.getInstanceFile()!!.name)
            val payload = formController.getFilledInFormXml()

            if (priority < lastPriorityUsed) {
                Timber.w("Savepoint thread (p=%d) was cancelled (b) because another one is waiting (p=%d)", priority, lastPriorityUsed)
                return null
            }

            // write out xml
            SaveFormToDisk.writeFile(payload, temp.absolutePath)

            val end = System.currentTimeMillis()
            Timber.i("Savepoint ms: %s to %s", (end - start).toString(), temp.toString())

            null
        } catch (e: Exception) {
            Timber.e(e.message)
            e.message
        }
    }

    override fun onPostExecute(result: String?) {
        if (result != null) {
            listener?.onSavePointError(result)
            listener = null
        }
    }

    override fun onProgressUpdate(vararg values: Unit) = Unit

    override fun onCancelled() = Unit

    companion object {
        private var lastPriorityUsed: Int = 0
    }
}
