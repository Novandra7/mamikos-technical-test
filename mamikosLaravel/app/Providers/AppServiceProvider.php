<?php

declare(strict_types=1);

namespace App\Providers;

use App\Events\AvailabilityInquiryCreated;
use App\Listeners\NotifyOwnerOfNewInquiry;
use App\Models\AvailabilityInquiry;
use App\Models\Kost;
use App\Observers\KostObserver;
use App\Policies\InquiryPolicy;
use App\Policies\KostPolicy;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Event;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\Facades\URL;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        //
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        Gate::policy(Kost::class, KostPolicy::class);
        Gate::policy(AvailabilityInquiry::class, InquiryPolicy::class);

        Kost::observe(KostObserver::class);

        Event::listen(AvailabilityInquiryCreated::class, NotifyOwnerOfNewInquiry::class);

        // PERF-02: surface N+1 queries loudly outside of production instead
        // of letting them silently degrade search performance.
        Model::preventLazyLoading(! $this->app->isProduction());
        Model::shouldBeStrict(! $this->app->isProduction());

        // SEC-14: force HTTPS URL generation once the app is deployed.
        if ($this->app->isProduction()) {
            URL::forceScheme('https');
        }

        $this->configureRateLimiters();
    }

    /**
     * SEC-08: baseline + endpoint-specific throttling.
     */
    private function configureRateLimiters(): void
    {
        RateLimiter::for('api', function (Request $request) {
            return Limit::perMinute(60)->by($request->user()?->id ?: $request->ip());
        });

        // 5 attempts / minute / IP+email — deliberately keyed on the
        // submitted email too, not just the IP, per US-02.
        RateLimiter::for('auth', function (Request $request) {
            return Limit::perMinute(5)->by($request->ip().'|'.mb_strtolower((string) $request->input('email')));
        });

        RateLimiter::for('inquiry', function (Request $request) {
            return Limit::perMinute(20)->by($request->user()?->id ?: $request->ip());
        });
    }
}
