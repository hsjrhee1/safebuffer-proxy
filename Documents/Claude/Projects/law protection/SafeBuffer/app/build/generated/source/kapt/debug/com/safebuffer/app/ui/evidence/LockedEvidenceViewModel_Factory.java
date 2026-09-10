package com.safebuffer.app.ui.evidence;

import com.safebuffer.app.data.local.ChunkDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class LockedEvidenceViewModel_Factory implements Factory<LockedEvidenceViewModel> {
  private final Provider<ChunkDao> chunkDaoProvider;

  public LockedEvidenceViewModel_Factory(Provider<ChunkDao> chunkDaoProvider) {
    this.chunkDaoProvider = chunkDaoProvider;
  }

  @Override
  public LockedEvidenceViewModel get() {
    return newInstance(chunkDaoProvider.get());
  }

  public static LockedEvidenceViewModel_Factory create(Provider<ChunkDao> chunkDaoProvider) {
    return new LockedEvidenceViewModel_Factory(chunkDaoProvider);
  }

  public static LockedEvidenceViewModel newInstance(ChunkDao chunkDao) {
    return new LockedEvidenceViewModel(chunkDao);
  }
}
