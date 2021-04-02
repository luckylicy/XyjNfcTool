package com.xyj.nfctool

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.xyj.nfctool.bean.DictBean
import com.xyj.nfctool.databinding.ActivityMainBinding
import java.nio.charset.StandardCharsets
import kotlin.system.measureTimeMillis

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val mDataBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(
            this,
            R.layout.activity_main
        )
    }

    var mDicts: MutableList<DictBean.DataBean> = mutableListOf()
    var mTag: Tag? = null

    private var mNfcAdapter: NfcAdapter? = null

    private val mLoadingDialog: LoadingDialog by lazy {
        LoadingDialog.Builder(this)
            .setShowMessage(false)
            .setCancelable(false)
            .setCancelOutside(false)
            .setMessage("加载中...")
            .create()
    }

    private val mDictRvAdapter by lazy {
        DictRvAdapter(R.layout.adapter_rv_dict, mDicts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDataBinding.lifecycleOwner = this

        M1CardUtils.setPendingIntent(
            PendingIntent.getActivity(
                this, 0, Intent(
                    this,
                    javaClass
                ), 0
            )
        )

        initRv()

        //读卡
        mDataBinding.btnRead.setOnClickListener {
            val time = measureTimeMillis {
                if (M1CardUtils.hasCardType(mTag, this, "MifareClassic")) {
                    try {
                        val stringBuilder = StringBuilder()
                        M1CardUtils.readCard(mTag)
                        val tagFirst = M1CardUtils.readCard(mTag, 1)
                        val tagSecond = M1CardUtils.readCard(mTag, 2)
                        val tags =
                            tagFirst.trim { it <= ' ' } + tagSecond.trim { it <= ' ' }
                        mDataBinding.tvContent.setText(tags)
                        Toast.makeText(this, "读取成功$tags", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "读取失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            Toast.makeText(this, "执行时间$time", Toast.LENGTH_SHORT).show()
        }

        // 写卡
        mDataBinding.btnWrite.setOnClickListener {
            writeToNfc(
                mDataBinding.etText.text.toString().trim()
            )
        }

    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter?.enableForegroundDispatch(this, M1CardUtils.getPendingIntent(), null, null)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        mNfcAdapter = M1CardUtils.isNfcAble(this)
        M1CardUtils.setPendingIntent(
            PendingIntent.getActivity(
                this, 0, Intent(
                    this,
                    javaClass
                ), 0
            )
        )
        Log.e("readCard", "onNewIntent")
        mTag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableForegroundDispatch(this)
    }


    private fun writeToNfc(content: String) {
        var content = content
        try {
            if (content.length >= 16) {
                val first = content.substring(0, 16)
                val bytesFirst =
                    first.toByteArray(StandardCharsets.UTF_8)
                val boolFirst = M1CardUtils.writeBlock(mTag, 1, bytesFirst)
                var second = content.substring(16)
                second = StringUtil.fillRight(second, " ", 16)
                val bytesSecond =
                    second.toByteArray(StandardCharsets.UTF_8)
                val boolSecond = M1CardUtils.writeBlock(mTag, 2, bytesSecond)
                if (boolFirst && boolSecond) {
                    mDataBinding.tvContent.setText(content)
                    Log.e("M1CardUtils", "写入成功")
                    Toast.makeText(this, "写入成功", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("M1CardUtils", "写入失败")
                    Toast.makeText(this, "写入失败", Toast.LENGTH_SHORT).show()
                }
            } else {
                content = StringUtil.fillRight(content, " ", 16)
                val bytes =
                    content.toByteArray(StandardCharsets.UTF_8)
                // 清除第二块区的数据
                val empty = StringUtil.fillRight("", " ", 16)
                val bytesEmpty =
                    empty.toByteArray(StandardCharsets.UTF_8)
                M1CardUtils.writeBlock(mTag, 2, bytesEmpty)
                if (M1CardUtils.writeBlock(mTag, 1, bytes)) {
                    mDataBinding.tvContent.setText(content)
                    Log.e("M1CardUtils", "写入成功")
                    Toast.makeText(this, "写入成功", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("M1CardUtils", "写入失败")
                    Toast.makeText(this, "写入失败", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(this, "请保持NFC标签与手机紧贴~", Toast.LENGTH_SHORT).show()
        } finally {
            hideLoadingDialog()
        }
    }

    private fun initRv() {
        getStrokeData()
        mDataBinding.rvDict.layoutManager = LinearLayoutManager(this)
        mDataBinding.rvDict.adapter = mDictRvAdapter

        mDataBinding.btnStroke.setOnClickListener {
            getStrokeData()
            mDictRvAdapter.notifyDataSetChanged()
        }

        mDataBinding.btnTrauma.setOnClickListener {
            getTraumaData()
            mDictRvAdapter.notifyDataSetChanged()
        }

        mDataBinding.btnChestPain.setOnClickListener {
            getChestPainData()
            mDictRvAdapter.notifyDataSetChanged()
        }

        mDictRvAdapter.setOnItemClickListener { adapter, view, position ->
            Log.d(TAG, mDicts[position].id + "   " + mDicts[position].value)
            Toast.makeText(
                this,
                mDicts[position].id + "   " + mDicts[position].value,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getStrokeData() {
        mDicts.clear()
        mDicts.add(DictBean.DataBean("出车时间", "depart120time"))
        mDicts.add(DictBean.DataBean("抵达现场时间", "arrivescenetime"))
        mDicts.add(DictBean.DataBean("离开现场时间", "levavescenetime"))
        mDicts.add(DictBean.DataBean("到达医院时间", "arrivehospitaltime"))
        mDicts.add(DictBean.DataBean("首次医疗接触时间", "fmctime"))
        mDicts.add(DictBean.DataBean("采血时间", "bloodcollectiontime"))
        mDicts.add(DictBean.DataBean("开始静脉溶栓时间", "thrombolyticstaticpushtime"))
    }

    private fun getTraumaData() {
        mDicts.clear()
        mDicts.add(DictBean.DataBean("出车时间", "depart120time"))
        mDicts.add(DictBean.DataBean("抵达现场时间", "arrivescenetime"))
        mDicts.add(DictBean.DataBean("离开现场时间", "levavescenetime"))
        mDicts.add(DictBean.DataBean("到达医院时间", "arrivehospitaltime"))
        mDicts.add(DictBean.DataBean("首次医疗接触时间", "fmctime"))
        mDicts.add(DictBean.DataBean("发病现场：静脉通路时间", "preemergencyvenouschanneltime"))
        mDicts.add(DictBean.DataBean("发病现场：气管插管时间", "preemergencytracheacannulatime"))
        mDicts.add(DictBean.DataBean("发病现场：心肺复苏时间", "preemergencycprtime"))
        mDicts.add(DictBean.DataBean("急诊现场：静脉通路时间", "inemergencyvenouschanneltime"))
        mDicts.add(DictBean.DataBean("急诊现场：气管插管时间", "inemergencytracheacannulatime"))
        mDicts.add(DictBean.DataBean("急诊现场：心肺复苏时间", "inemergencycprtime"))
    }

    private fun getChestPainData() {
        mDicts.clear()
        mDicts.add(DictBean.DataBean("出车时间", "depart120time"))
        mDicts.add(DictBean.DataBean("抵达现场时间", "arrivescenetime"))
        mDicts.add(DictBean.DataBean("离开现场时间", "levavescenetime"))
        mDicts.add(DictBean.DataBean("到达医院时间", "arrivehospitaltime"))
        mDicts.add(DictBean.DataBean("首次医疗接触时间", "fmctime"))
        mDicts.add(DictBean.DataBean("开始静脉溶栓时间", "afterthrombolysisbegintime"))
        mDicts.add(DictBean.DataBean("初始药物：阿司匹林", "acsaspirintime"))
        mDicts.add(DictBean.DataBean("初始药物：氯呲格雷", "acschlorpyridintime"))
        mDicts.add(DictBean.DataBean("初始药物：替格瑞洛", "acstigrilotime"))
        mDicts.add(DictBean.DataBean("初始药物：术前抗凝", "acsanticoagulantmedicinetime"))
    }


    fun showLoadingDialog() {
        if (mLoadingDialog.isShowing()) {
            return
        }
        mLoadingDialog.show()
    }

    fun hideLoadingDialog() {
        mLoadingDialog.dismiss()
    }
}

