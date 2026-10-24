package ca.mgamble.mist;

import ca.mgamble.mist.model.ApiUsage;
import ca.mgamble.mist.model.SelfAccount;

/** Endpoints about the token / admin making the requests. Obtain via {@link MistClient#self()}. */
public final class SelfApi {

    private final Requests requests;

    SelfApi(Requests requests) {
        this.requests = requests;
    }

    /** {@code getSelf}: the current admin and its privileges (handy for discovering accessible orgs). */
    public SelfAccount get() {
        return requests.get("/api/v1/self", SelfAccount.class);
    }

    /** {@code getSelfApiUsage}: calls made this hour against the token's hourly limit. */
    public ApiUsage apiUsage() {
        return requests.get("/api/v1/self/usage", ApiUsage.class);
    }
}
