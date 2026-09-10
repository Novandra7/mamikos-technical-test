<?php

declare(strict_types=1);

namespace App\Http\Resources;

use App\Models\CreditBalance;
use App\Services\CreditService;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * Backs GET /me/credits. Expects the `user` relation to be loaded so the
 * role-based quota and inquiry allowance can be computed (US-09).
 *
 * @mixin CreditBalance
 */
class CreditBalanceResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        /** @var CreditService $creditService */
        $creditService = app(CreditService::class);

        $role = $this->user->role;
        $quota = $creditService->quotaFor($role);
        $inquiryCost = (int) config('credit.inquiry_cost', 5);
        $timezone = config('credit.timezone', 'Asia/Jakarta');

        return [
            'role' => $role->value,
            'balance' => $this->balance,
            'quota' => $quota,
            'inquiry_cost' => $inquiryCost,
            'remaining_inquiries' => intdiv($this->balance, max($inquiryCost, 1)),
            'last_recharged_at' => $this->last_recharged_at?->toIso8601String(),
            'next_recharge_at' => now($timezone)->addMonthNoOverflow()->startOfMonth()->toIso8601String(),
        ];
    }
}
