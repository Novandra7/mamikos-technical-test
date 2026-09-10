<?php

declare(strict_types=1);

namespace App\Console\Commands;

use App\Enums\RechargeStrategyType;
use App\Services\CreditRechargeService;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Cache;

class RechargeUserCreditsCommand extends Command
{
    protected $signature = 'credits:recharge
        {--dry-run : Compute the outcome without persisting any change}
        {--user-id= : Only process a single user, useful for manual verification}
        {--strategy= : Override the configured recharge strategy (reset|topup)}
        {--force : Ignore the idempotency check and recharge even if already done this period}';

    protected $description = 'Recharge REGULAR/PREMIUM credit wallets according to the configured monthly strategy';

    public function handle(CreditRechargeService $service): int
    {
        $lock = Cache::lock('credits:recharge', 900);

        if (! $lock->get()) {
            $this->warn('Another recharge run is in progress. Skipping.');

            return self::SUCCESS;
        }

        try {
            $strategy = RechargeStrategyType::from(
                $this->option('strategy') ?? config('credit.recharge_strategy', 'reset')
            );

            $userId = $this->option('user-id') !== null ? (int) $this->option('user-id') : null;

            $summary = $service->rechargeAll(
                strategy: $strategy,
                dryRun: (bool) $this->option('dry-run'),
                userId: $userId,
                force: (bool) $this->option('force'),
            );

            $this->table(
                ['Processed', 'Recharged', 'Skipped', 'Failed', 'Duration (s)'],
                [$summary->toRow()],
            );

            return $summary->failed > 0 ? self::FAILURE : self::SUCCESS;
        } finally {
            $lock->release();
        }
    }
}
