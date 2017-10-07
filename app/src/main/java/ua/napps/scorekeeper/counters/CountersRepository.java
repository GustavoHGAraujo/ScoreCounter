package ua.napps.scorekeeper.counters;

import android.arch.lifecycle.LiveData;
import io.reactivex.Completable;
import java.util.List;

public interface CountersRepository {

  LiveData<List<Counter>> getCounters();

  LiveData<Counter> loadCounter(int counterId);

  Completable createCounter(String name);

  Completable modifyCount(int counterId, int difference);

  Completable setCount(int counterId, int value);

  Completable modifyName(int counterId, String name);

  Completable modifyDefaultValue(int counterId, int defaultValue);

  Completable modifyStep(int counterId, int step);

  Completable modifyColor(int counterId, String hex);

  Completable delete(Counter counter);
}
