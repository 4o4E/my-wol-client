# my-wol-client

简单的 walk-on-lan 客户端

> 因为有远程开机的需求，正好想玩一下kmp，所以就写了这个小工具

> 在我自己使用的情况下, 我有公网服务器但是我家里没有多余的机器也没有公网, 于是我可以用旧手机连接服务器, 通过服务器唤醒家里的设备

## 特性

- [x] 支持直接唤醒局域网内的设备
- [x] 支持通过[服务器](https://github.com/4o4E/my-wol-backend)唤醒远程设备
- [x] 支持通过ssh链接至设备并发送指令
- [x] 设置定时任务唤醒设备

## 使用

### ssh 注意事项

win上设置 `ssh server` 后需要授权 `icacls.exe "C:\Users\username\.ssh\authorized_keys" /inheritance:r /grant "Administrators:F" /grant "SYSTEM:F"`

win上 `ssh server` 配置(`C:\ProgramData\ssh\sshd_config`)需要启用密钥登录(`PubkeyAuthentication yes`)