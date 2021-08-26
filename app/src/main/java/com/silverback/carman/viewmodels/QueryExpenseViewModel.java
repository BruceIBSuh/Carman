package com.silverback.carman.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman.database.CarmanDatabase;

import java.util.List;

public class QueryExpenseViewModel extends ViewModel {

    private MutableLiveData<List<Integer>> expLiveData;
    private MutableLiveData<String> expenseTime;

    public LiveData<List<Integer>> getMonthlyExpense() {
        if(expLiveData == null) {
            expLiveData = new MutableLiveData<>();

        }

        return expLiveData;
    }

    public LiveData<String> getExpenseTime() {
        if(expenseTime == null) expenseTime = new MutableLiveData<>();
        return expenseTime;
    }

    public void setMonthlyExpense(List<Integer> expense) {
        expLiveData.setValue(expense);
    }

    /*
    public void setExpenseTime(String time) {
        expenseTime.setValue(time);
    }

     */

    private void loadMonthlyExpense() {

    }
}
