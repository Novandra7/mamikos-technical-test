<?php

declare(strict_types=1);

namespace App\Providers;

use App\Repositories\Contracts\CreditRepositoryInterface;
use App\Repositories\Contracts\InquiryRepositoryInterface;
use App\Repositories\Contracts\KostRepositoryInterface;
use App\Repositories\Eloquent\EloquentCreditRepository;
use App\Repositories\Eloquent\EloquentInquiryRepository;
use App\Repositories\Eloquent\EloquentKostRepository;
use Illuminate\Support\ServiceProvider;

/**
 * Services depend on repository interfaces, never the Eloquent
 * implementation directly (see PRD §13.2) — this is what lets unit tests
 * mock persistence instead of touching the database.
 */
class RepositoryServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->bind(KostRepositoryInterface::class, EloquentKostRepository::class);
        $this->app->bind(CreditRepositoryInterface::class, EloquentCreditRepository::class);
        $this->app->bind(InquiryRepositoryInterface::class, EloquentInquiryRepository::class);
    }
}
