export interface RegisterRequest {
    username : string;
    email : string;
    password: string;
}

export interface AuthenticationRequest {
    username : string;
    password : string;
}

export interface AuthenticationResponse{
    token : string;
}