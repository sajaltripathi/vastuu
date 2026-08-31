package com.tiscan.app;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;

/* JADX INFO: compiled from: D8$$SyntheticClass */
/* JADX INFO: loaded from: classes3.dex */
@LambdaMethod(holder = "Lcom/tiscan/app/ARScanActivity;", method = "refreshMeasure", proto = "()V")
@SynthesizedClassV2(apiLevel = -2, kind = 28, versionHash = "3b119036505a817327d30bbe4a430e8676906c2ab0a3363856806bcd3b289007")
public final /* synthetic */ class ARScanActivity$$ExternalSyntheticLambda2 implements Runnable {
    public final /* synthetic */ ARScanActivity f$0;

    public /* synthetic */ ARScanActivity$$ExternalSyntheticLambda2(ARScanActivity aRScanActivity) {
        this.f$0 = aRScanActivity;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.refreshMeasure();
    }
}
