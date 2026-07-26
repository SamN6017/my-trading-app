import { Routes } from '@angular/router';
import { AuthLogin } from './auth/components/auth-login/auth-login';
import { AuthRegister } from './auth/components/auth-register/auth-register';
import { StockList } from './components/stock-list/stock-list';

export const routes: Routes = [
    {path: 'login', component:AuthLogin},
    {path: 'register', component:AuthRegister},
    {path: 'stock-list', component:StockList},
    {path: '', redirectTo: 'login', pathMatch: 'full'},
];
