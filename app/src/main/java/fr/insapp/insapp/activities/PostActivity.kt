package fr.insapp.insapp.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.util.Linkify
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.like.LikeButton
import com.like.OnLikeListener
import fr.insapp.insapp.R
import fr.insapp.insapp.adapters.CommentRecyclerViewAdapter
import fr.insapp.insapp.http.ServiceGenerator
import fr.insapp.insapp.listeners.PostCommentLongClickListener
import fr.insapp.insapp.models.*
import fr.insapp.insapp.utility.Utils
import kotlinx.android.synthetic.main.activity_post.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by thomas on 12/11/2016.
 */

class PostActivity : AppCompatActivity() {

    private lateinit var adapter: CommentRecyclerViewAdapter

    private lateinit var post: Post
    private var club: Club? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val user = Utils.user

        // toolbar

        setSupportActionBar(post_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // post

        if (intent.getParcelableExtra<Post>("post") != null) {
            // coming from navigation

            this.post = intent.getParcelableExtra("post")
            generateActivity()

            // mark notification as seen

            if (intent.getParcelableExtra<Notification>("notification") != null) {
                val notification = intent.getParcelableExtra<Notification>("notification")

                val call = ServiceGenerator.create().markNotificationAsSeen(user?.id, notification.id)
                call.enqueue(object : Callback<Notifications> {
                    override fun onResponse(call: Call<Notifications>, response: Response<Notifications>) {
                        if (!response.isSuccessful) {
                            Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<Notifications>, t: Throwable) {
                        Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                    }
                })
            }
        } else {
            // coming from notification

            val call = ServiceGenerator.create().getPostFromId(intent.getStringExtra("ID"))
            call.enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (response.isSuccessful) {
                        post = response.body()!!
                        generateActivity()
                    }
                    else
                        Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun generateActivity() {
        val user = Utils.user

        // Answers

        Answers.getInstance().logContentView(ContentViewEvent()
                .putContentId(post.id)
                .putContentName(post.title)
                .putContentType("Post")
                .putCustomAttribute("Favorites count", post.likes?.size ?: 0)
                .putCustomAttribute("Comments count", post.comments?.size ?: 0))

        // hide image if necessary

        if (post.imageSize == null || post.image.isEmpty()) {
            post_placeholder?.visibility = View.GONE
            post_image?.visibility = View.GONE
        }

        // like button

        post_like_button?.isLiked = post.isPostLikedBy(user?.id)
        post_like_counter?.text = post.likes?.size?.toString() ?: "0"

        post_like_button?.setOnLikeListener(object : OnLikeListener {
            override fun liked(likeButton: LikeButton) {
                post_like_counter?.text = (Integer.valueOf(post_like_counter?.text as String) + 1).toString()

                val call = ServiceGenerator.create().likePost(post.id, user?.id)
                call.enqueue(object : Callback<PostInteraction> {
                    override fun onResponse(call: Call<PostInteraction>, response: Response<PostInteraction>) {
                        if (response.isSuccessful) {
                            post = response.body()!!.post
                        } else {
                            Toast.makeText(this@PostActivity, "PostRecyclerViewAdapter", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<PostInteraction>, t: Throwable) {
                        Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                    }
                })
            }

            override fun unLiked(likeButton: LikeButton) {
                post_like_counter?.text = (Integer.valueOf(post_like_counter?.text as String) - 1).toString()

                if (Integer.valueOf(post_like_counter?.text as String) < 0) {
                    post_like_counter?.text = "0"
                }

                val call = ServiceGenerator.create().dislikePost(post.id, user?.id)
                call.enqueue(object : Callback<PostInteraction> {
                    override fun onResponse(call: Call<PostInteraction>, response: Response<PostInteraction>) {
                        if (response.isSuccessful) {
                            post = response.body()!!.post
                        } else {
                            Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<PostInteraction>, t: Throwable) {
                        Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                    }
                })
            }
        })

        val call = ServiceGenerator.create().getClubFromId(post.association)
        call.enqueue(object : Callback<Club> {
            override fun onResponse(call: Call<Club>, response: Response<Club>) {
                if (response.isSuccessful) {
                    club = response.body()

                    Glide
                        .with(this@PostActivity)
                        .load(ServiceGenerator.CDN_URL + club!!.profilePicture)
                        .apply(RequestOptions.circleCropTransform())
                        .transition(withCrossFade())
                        .into(post_club_avatar)

                    // listener

                    post_club_avatar?.setOnClickListener { startActivity(Intent(this@PostActivity, ClubActivity::class.java).putExtra("club", club)) }
                } else {
                    Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Club>, t: Throwable) {
                Toast.makeText(this@PostActivity, "PostActivity", Toast.LENGTH_LONG).show()
            }
        })

        post_title?.text = post.title
        post_text?.text = post.description
        post_date?.text = Utils.displayedDate(post.date)

        // view links contained in description

        Linkify.addLinks(post_text, Linkify.ALL)
        Utils.convertToLinkSpan(this@PostActivity, post_text)

        // adapter

        this.adapter = CommentRecyclerViewAdapter(this@PostActivity, Glide.with(this), post.comments)
        adapter.setOnItemLongClickListener(PostCommentLongClickListener(this@PostActivity, post, adapter))

        // edit comment

        comment_post_input.setupComponent(adapter, post)

        // recycler view

        recyclerview_comments_post.setHasFixedSize(true)
        recyclerview_comments_post.isNestedScrollingEnabled = false

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerview_comments_post.layoutManager = layoutManager

        recyclerview_comments_post.adapter = adapter

        // retrieve the avatar of the user

        val id = resources.getIdentifier(Utils.drawableProfileName(user?.promotion, user?.gender), "drawable", packageName)
        Glide
            .with(this@PostActivity)
            .load(id)
            .transition(withCrossFade())
            .apply(RequestOptions.circleCropTransform())
            .into(comment_post_username_avatar)

        // image

        if (post.imageSize != null && !post.image.isEmpty()) {
            post_placeholder?.setImageSize(post.imageSize)

            Glide
                .with(this@PostActivity)
                .load(ServiceGenerator.CDN_URL + post.image)
                .transition(withCrossFade())
                .into(post_image)
        }
    }

    override fun finish() {
        val sendIntent = Intent()
        sendIntent.putExtra("post", post)

        setResult(Activity.RESULT_OK, sendIntent)

        super.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isTaskRoot) {
                    startActivity(Intent(this@PostActivity, MainActivity::class.java))
                } else {
                    finish()
                }
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
