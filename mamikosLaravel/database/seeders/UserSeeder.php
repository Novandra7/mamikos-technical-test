<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Enums\UserRole;
use App\Models\User;
use App\Services\CreditService;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

/**
 * Seeds the demo accounts from PRD §8.3, used both for the demo script in
 * §20 and as fixtures reviewers can log in with immediately.
 */
class UserSeeder extends Seeder
{
    public function run(): void
    {
        /** @var CreditService $creditService */
        $creditService = app(CreditService::class);

        $accounts = [
            ['name' => 'Owner Satu', 'email' => 'owner1@mamikos.test', 'role' => UserRole::OWNER, 'phone' => '081200000001'],
            ['name' => 'Owner Dua', 'email' => 'owner2@mamikos.test', 'role' => UserRole::OWNER, 'phone' => '081200000002'],
            ['name' => 'Regular User', 'email' => 'regular@mamikos.test', 'role' => UserRole::REGULAR, 'phone' => '081200000003'],
            ['name' => 'Premium User', 'email' => 'premium@mamikos.test', 'role' => UserRole::PREMIUM, 'phone' => '081200000004'],
            ['name' => 'Low Credit User', 'email' => 'lowcredit@mamikos.test', 'role' => UserRole::REGULAR, 'phone' => '081200000005'],
        ];

        foreach ($accounts as $account) {
            $user = User::updateOrCreate(
                ['email' => $account['email']],
                [
                    'name' => $account['name'],
                    'password' => Hash::make('Password123'),
                    'role' => $account['role'],
                    'phone' => $account['phone'],
                ],
            );

            if ($account['role']->hasCreditWallet() && $user->creditBalance === null) {
                $creditService->grantInitial($user->id, $account['role']);
            }
        }

        // The "low credit" demo account exists specifically to exercise
        // the INSUFFICIENT_CREDIT path (US-08 AC) without needing to burn
        // through a full 20-credit balance first.
        $lowCreditUser = User::where('email', 'lowcredit@mamikos.test')->firstOrFail();
        $lowCreditUser->creditBalance()->update(['balance' => 3]);
    }
}
