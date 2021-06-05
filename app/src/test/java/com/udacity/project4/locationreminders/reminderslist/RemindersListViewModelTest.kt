package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    val list = listOf<ReminderDTO>(
        ReminderDTO("title", "description", "location", 0.0, 0.0),
        ReminderDTO(
            "title",
            "description",
            "location",
            (-360..360).random().toDouble(),
            (-360..360).random().toDouble()
        ),
        ReminderDTO(
            "title",
            "description",
            "location",
            (-360..360).random().toDouble(),
            (-360..360).random().toDouble()
        ),
        ReminderDTO(
            "title",
            "description",
            "location",
            (-360..360).random().toDouble(),
            (-360..360).random().toDouble()
        )
    )
    private val reminder1 = list[0]
    private val reminder2 = list[1]
    private val reminder3 = list[2]

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var reminderListViewModel: RemindersListViewModel

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun getRemindersList() {
        val remindersList = mutableListOf(reminder1, reminder2, reminder3)
        fakeDataSource = FakeDataSource(remindersList)
        reminderListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        reminderListViewModel.loadReminders()
        Assert.assertThat(
            reminderListViewModel.remindersList.getOrAwaitValue(),
            (IsNot.not(emptyList()))
        )
        Assert.assertThat(
            reminderListViewModel.remindersList.getOrAwaitValue().size,
            CoreMatchers.`is`(remindersList.size)
        )
    }

    @Test
    fun check_loading() {
        fakeDataSource = FakeDataSource(mutableListOf())
        reminderListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        mainCoroutineRule.pauseDispatcher()
        reminderListViewModel.loadReminders()
        Assert.assertThat(
            reminderListViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun returnError() {
        fakeDataSource = FakeDataSource(null)
        reminderListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        reminderListViewModel.loadReminders()
        Assert.assertThat(
            reminderListViewModel.showSnackBar.getOrAwaitValue(),
            CoreMatchers.`is`("No reminders found")
        )
    }


}